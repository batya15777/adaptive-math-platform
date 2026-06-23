import { useState, useEffect, useCallback } from "react";
import {
    askTutor,
    getTutorMessages,
    getTutorConversations,
} from "../../service/tutorChatApi.js";
import {
    DEFAULT_LANGUAGE,
    getTutorChatStrings,
    isRtlLanguage,
    localeFor,
} from "./tutorChatStrings.js";
import "./TutorChat.css";

// A saved exchange row -> two display turns (student + tutor).
const expandRows = (rows) =>
    (rows || []).flatMap((row) => [
        { role: "student", content: row.studentMessage },
        {
            role: "tutor",
            content: row.tutorResponse,
            guidanceLevel: row.guidanceLevel,
            action: row.action,
        },
    ]);

const formatDate = (value, locale) => {
    if (!value) return "";
    const date = new Date(value);
    return Number.isNaN(date.getTime())
        ? ""
        : date.toLocaleString(locale, { dateStyle: "short", timeStyle: "short" });
};

// Math expressions like "5x - 1 = -6" or "2(x + 3) = 10" get reordered inside RTL (Hebrew)
// text because the signs/parens are bidi-neutral. We find each math run and wrap it in an
// LTR-isolated span so it always reads left-to-right, while the surrounding Hebrew keeps its
// own direction. The character class excludes Hebrew (א-ת) so a run can never swallow Hebrew
// text, and we only WRAP a run when it truly looks like an expression (has an operator / '=' /
// power / leading minus) — so lone numbers ("יש לך 3 ניסיונות") and plain words stay untouched.
// The lookbehind skips a Hebrew-prefixed hyphen (e.g. "ל-10") so connectors aren't mistaken
// for a math minus. This only affects display — the original message text is never changed.
const MATH_CANDIDATE = /(?<![\wא-ת])-?[0-9A-Za-z(][0-9A-Za-z+\-*/×÷=^().\s]*[0-9A-Za-z)]|(?<![\wא-ת])-\d+(?:\.\d+)?/g;
const looksLikeMath = (s) =>
    /[=+×÷^]|\d\s*[-*/]\s*\d|[A-Za-z]\s*[-+*/=]/.test(s) || /^-\d/.test(s.trim());

// The tutor LLM often wraps math in LaTeX it didn't need to ( \( … \) , \[ … \] , $…$ , \times ),
// which the chat shows as literal backslashes/markup. Unwrap it for DISPLAY only — the stored
// message text is never changed. Real parentheses (e.g. "2(x + 3)") are kept; only the
// backslash-escaped delimiters/commands are removed.
const stripInlineLatex = (text) =>
    String(text ?? '')
        .replace(/\\times/g, '×').replace(/\\div/g, '÷').replace(/\\cdot/g, '·')
        .replace(/\\[()[\]]/g, '')   // \( \) \[ \]
        .replace(/\${1,2}/g, '')      // $ , $$
        .replace(/\\[,;:!]/g, ' ')    // LaTeX thin-spaces
        .replace(/\\/g, '')           // any remaining stray backslash
        .replace(/[ \t]{2,}/g, ' ')   // collapse the double spaces left behind
        .replace(/ +([.,;:!?])/g, '$1'); // and the stray space before punctuation

const renderContent = (rawText) => {
    const text = stripInlineLatex(rawText);
    const regex = new RegExp(MATH_CANDIDATE);
    const parts = [];
    let lastIndex = 0;
    let key = 0;
    let match;
    while ((match = regex.exec(text)) !== null) {
        if (match.index > lastIndex) {
            parts.push(text.slice(lastIndex, match.index));
        }
        // Real expression → isolate it LTR; otherwise keep it as plain text so words and
        // lone numbers are never reordered or restyled.
        parts.push(
            looksLikeMath(match[0])
                ? (
                    <span key={`expr-${key++}`} className="tutor-chat__expr" dir="ltr">
                        {match[0]}
                    </span>
                )
                : match[0],
        );
        lastIndex = match.index + match[0].length;
    }
    if (lastIndex < text.length) {
        parts.push(text.slice(lastIndex));
    }
    return parts;
};

function TutorChat({
    questionId,
    language = DEFAULT_LANGUAGE,
    title,
    placeholder,
    className = "",
    onResponse,
}) {
    const strings = getTutorChatStrings(language);
    const dir = isRtlLanguage(language) ? "rtl" : "ltr";
    const locale = localeFor(language);
    const headerTitle = title ?? strings.title;
    const inputPlaceholder = placeholder ?? strings.placeholder;

    const [messages, setMessages] = useState([]);
    const [message, setMessage] = useState("");
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState("");

    // mode: "chat" (live) | "list" (history list) | "viewing" (a past conversation, read-only)
    const [mode, setMode] = useState("chat");
    const [conversations, setConversations] = useState([]);
    const [conversationsLoading, setConversationsLoading] = useState(false);
    const [viewing, setViewing] = useState(null);

    const canSend = message.trim().length > 0 && questionId && !isLoading;

    // Restore this question's saved conversation on mount (and whenever the question changes).
    useEffect(() => {
        if (!questionId) return undefined;
        let active = true;
        getTutorMessages(questionId)
            .then((response) => {
                if (active) setMessages(expandRows(response.data));
            })
            .catch(() => {
                // No saved history yet, or it could not load — start with an empty chat.
                if (active) setMessages([]);
            });
        return () => {
            active = false;
        };
    }, [questionId]);

    const openHistory = useCallback(() => {
        setMode("list");
        setConversationsLoading(true);
        getTutorConversations()
            .then((response) => setConversations(response.data || []))
            .catch(() => setConversations([]))
            .finally(() => setConversationsLoading(false));
    }, []);

    const openConversation = useCallback((conversation) => {
        setViewing({
            questionId: conversation.questionId,
            expression: conversation.expression,
            messages: [],
            loading: true,
            error: "",
        });
        setMode("viewing");
        getTutorMessages(conversation.questionId)
            .then((response) =>
                setViewing((current) =>
                    current ? { ...current, messages: expandRows(response.data), loading: false } : current,
                ),
            )
            .catch(() =>
                setViewing((current) =>
                    current ? { ...current, loading: false, error: strings.errLoadConversation } : current,
                ),
            );
    }, [strings.errLoadConversation]);

    const handleHeaderButton = () => {
        if (mode === "chat") openHistory();
        else if (mode === "viewing") setMode("list");
        else setMode("chat");
    };

    const handleSubmit = async (event) => {
        event.preventDefault();

        const trimmedMessage = message.trim();
        if (!trimmedMessage || !questionId) {
            return;
        }

        const studentMessage = {
            role: "student",
            content: trimmedMessage,
        };

        setMessages((currentMessages) => [...currentMessages, studentMessage]);
        setMessage("");
        setError("");
        setIsLoading(true);

        try {
            const response = await askTutor({
                questionId,
                message: trimmedMessage,
            });

            const tutorMessage = {
                role: "tutor",
                content: response.data.message,
                guidanceLevel: response.data.guidanceLevel,
                action: response.data.action,
            };

            setMessages((currentMessages) => [...currentMessages, tutorMessage]);
            onResponse?.(response.data);
        } catch (requestError) {
            const status = requestError.response?.status;
            const backendMessage =
                requestError.response?.data?.message ||
                requestError.response?.data?.error ||
                requestError.response?.data?.detail ||
                requestError.message;

            console.error("Tutor chat request failed", {
                status,
                backendMessage,
                data: requestError.response?.data,
                requestError,
            });

            if (status === 404) {
                setError(strings.errNotFound);
            } else if (status === 401 || status === 403) {
                setError(strings.errAuth);
            } else if (status === 400) {
                setError(strings.errBadRequest);
            } else {
                setError(`${strings.errGeneric}${backendMessage ? `: ${backendMessage}` : ""}`);
            }
        } finally {
            setIsLoading(false);
        }
    };

    const renderMessages = (list, emptyText) =>
        list.length === 0 ? (
            <div className="tutor-chat__empty">
                <p>{emptyText}</p>
            </div>
        ) : (
            list.map((chatMessage, index) => (
                <article
                    className={`tutor-chat__message tutor-chat__message--${chatMessage.role}`}
                    key={`${chatMessage.role}-${index}`}
                >
                    <span className="tutor-chat__sender">
                        {chatMessage.role === "student" ? strings.senderStudent : strings.senderTutor}
                    </span>
                    <p>{renderContent(chatMessage.content)}</p>
                </article>
            ))
        );

    return (
        <section className={`tutor-chat ${className}`} dir={dir} aria-label="AI tutor chat">
            <header className="tutor-chat__header">
                <div>
                    <p className="tutor-chat__eyebrow">{strings.eyebrow}</p>
                    <h2>{headerTitle}</h2>
                </div>
                <button type="button" className="tutor-chat__history-btn" onClick={handleHeaderButton}>
                    {mode === "chat" ? strings.history : strings.back}
                </button>
            </header>

            {mode === "list" && (
                <div className="tutor-chat__messages">
                    {conversationsLoading ? (
                        <p className="tutor-chat__loading-text">{strings.loadingConversations}</p>
                    ) : conversations.length === 0 ? (
                        <div className="tutor-chat__empty">
                            <p>{strings.noConversations}</p>
                        </div>
                    ) : (
                        <ul className="tutor-chat__conv-list">
                            {conversations.map((conversation) => (
                                <li key={conversation.questionId}>
                                    <button
                                        type="button"
                                        className="tutor-chat__conv-item"
                                        onClick={() => openConversation(conversation)}
                                    >
                                        <span className="tutor-chat__conv-expr" dir="ltr">{conversation.expression}</span>
                                        <span className="tutor-chat__conv-meta">
                                            {conversation.messageCount} {strings.messages} · {formatDate(conversation.lastMessageAt, locale)}
                                        </span>
                                    </button>
                                </li>
                            ))}
                        </ul>
                    )}
                </div>
            )}

            {mode === "viewing" && viewing && (
                <div className="tutor-chat__messages">
                    <div className="tutor-chat__viewing-banner">{strings.viewingPrefix}{viewing.expression}</div>
                    {viewing.loading ? (
                        <p className="tutor-chat__loading-text">{strings.loading}</p>
                    ) : viewing.error ? (
                        <p className="tutor-chat__error">{viewing.error}</p>
                    ) : (
                        renderMessages(viewing.messages, strings.emptyConversation)
                    )}
                </div>
            )}

            {mode === "chat" && (
                <>
                    <div className="tutor-chat__messages">
                        {renderMessages(messages, strings.emptyChat)}

                        {isLoading && (
                            <div className="tutor-chat__message tutor-chat__message--tutor">
                                <span className="tutor-chat__sender">{strings.senderTutor}</span>
                                <p>{strings.thinking}</p>
                            </div>
                        )}
                    </div>

                    {error && <p className="tutor-chat__error">{error}</p>}

                    <form className="tutor-chat__form" onSubmit={handleSubmit}>
                        <textarea
                            value={message}
                            placeholder={inputPlaceholder}
                            onChange={(event) => setMessage(event.target.value)}
                            disabled={!questionId || isLoading}
                            rows={3}
                        />
                        <button type="submit" disabled={!canSend}>
                            {strings.send}
                        </button>
                    </form>

                    {!questionId && <p className="tutor-chat__hint">{strings.noQuestion}</p>}
                </>
            )}
        </section>
    );
}

export default TutorChat;
