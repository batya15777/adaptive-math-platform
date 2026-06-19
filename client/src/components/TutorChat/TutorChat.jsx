import { useState } from "react";
import { askTutor } from "../../service/tutorChatApi.js";
import "./TutorChat.css";

function TutorChat({
    questionId,
    title = "AI Tutor",
    placeholder = "כתבי שאלה על התרגיל...",
    className = "",
    onResponse,
}) {
    const [messages, setMessages] = useState([]);
    const [message, setMessage] = useState("");
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState("");

    const canSend = message.trim().length > 0 && questionId && !isLoading;

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
                setError("לא נמצאה שאלה במערכת עבור הצ׳אט. צריך להוסיף שאלה לטבלה או להעביר מזהה שאלה קיים.");
            } else if (status === 401 || status === 403) {
                setError("צריך להתחבר מחדש כדי להשתמש בצ׳אט.");
            } else if (status === 400) {
                setError("חסר מזהה שאלה או טקסט שאלה לצ׳אט.");
            } else {
                setError(`לא הצלחתי לקבל תשובה כרגע${backendMessage ? `: ${backendMessage}` : ""}`);
            }
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <section className={`tutor-chat ${className}`} dir="rtl" aria-label="AI tutor chat">
            <header className="tutor-chat__header">
                <div>
                    <p className="tutor-chat__eyebrow">עזרה בתרגיל</p>
                    <h2>{title}</h2>
                </div>
            </header>

            <div className="tutor-chat__messages">
                {messages.length === 0 ? (
                    <div className="tutor-chat__empty">
                        <p>נתקעת? שאלי אותי שאלה על התרגיל ואכוון אותך צעד אחרי צעד.</p>
                    </div>
                ) : (
                    messages.map((chatMessage, index) => (
                        <article
                            className={`tutor-chat__message tutor-chat__message--${chatMessage.role}`}
                            key={`${chatMessage.role}-${index}`}
                        >
                            <span className="tutor-chat__sender">
                                {chatMessage.role === "student" ? "אני" : "המדריך"}
                            </span>
                            <p>{chatMessage.content}</p>
                        </article>
                    ))
                )}

                {isLoading && (
                    <div className="tutor-chat__message tutor-chat__message--tutor">
                        <span className="tutor-chat__sender">המדריך</span>
                        <p>רגע, אני חושב איך להדריך אותך...</p>
                    </div>
                )}
            </div>

            {error && <p className="tutor-chat__error">{error}</p>}

            <form className="tutor-chat__form" onSubmit={handleSubmit}>
                <textarea
                    value={message}
                    placeholder={placeholder}
                    onChange={(event) => setMessage(event.target.value)}
                    disabled={!questionId || isLoading}
                    rows={3}
                />
                <button type="submit" disabled={!canSend}>
                    שלח
                </button>
            </form>

            {!questionId && (
                <p className="tutor-chat__hint">כדי להפעיל את הצ׳אט צריך להעביר לו מזהה שאלה.</p>
            )}
        </section>
    );
}

export default TutorChat;
