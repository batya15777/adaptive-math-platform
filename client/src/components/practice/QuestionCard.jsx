import { useState } from 'react';
import { format } from '../../i18n/languages.js';
import { parseOptions, parseSolution } from '../../utils/questionFormat.js';
import './QuestionCard.css';

// Presentational question card — difficulty label, expression (kept LTR even in RTL),
// answer area (multiple-choice grid OR free-text input), and an inline feedback line.
// Purely controlled by props; reusable for any question-answering flow.
//   question       the question object from the API (expression, options, difficultyLevel…)
//   t              questionGameStrings dictionary (Submit / difficulty / answerPlaceholder…)
//   feedback       { type: 'correct'|'wrong'|'failed', text } | null
//   locked         disable inputs (busy or already concluded)
//   onAnswer       (value: string) => void
//   meta           optional extra text after the difficulty label (e.g. "attempt 1/3")
//   showSolution   auto-reveal the worked solution on 'failed' (default true). Set false
//                  when the parent renders its own staged hints/solution via `footer`.
//   footer         optional node rendered at the bottom of the card (hint/next buttons…)
export function QuestionCard({
    question, t, feedback, locked = false, onAnswer,
    meta = null, showSolution = true, footer = null,
}) {
    const [value, setValue] = useState('');
    const options = parseOptions(question?.options);
    const isMultipleChoice = options.length > 0;
    // When the student runs out of tries (feedback 'failed'), reveal the worked solution —
    // unless the parent opted out (showSolution=false) to drive its own staged reveal.
    const solutionSteps = showSolution && feedback?.type === 'failed' ? parseSolution(question?.solution) : [];

    const submit = (v) => {
        const given = (v ?? value).toString().trim();
        if (!given || locked) return;
        onAnswer(given);
    };

    return (
        <div className="qc">
            <div className="qc-diff">
                {format(t.difficulty, { level: question?.difficultyLevel ?? 1 })}
                {meta != null && meta !== '' && <span className="qc-diff-meta"> · {meta}</span>}
            </div>
            <div className="qc-expr qc-math">{question?.expression}</div>

            {isMultipleChoice ? (
                <div className="qc-options">
                    {options.map(opt => (
                        <button key={opt} type="button" className="qc-option qc-math" disabled={locked} onClick={() => submit(opt)}>
                            {opt}
                        </button>
                    ))}
                </div>
            ) : (
                <div className="qc-input-row">
                    <input
                        className="qc-input qc-math"
                        type="text"
                        value={value}
                        disabled={locked}
                        onChange={e => setValue(e.target.value)}
                        onKeyDown={e => { if (e.key === 'Enter') submit(); }}
                        placeholder={t.answerPlaceholder}
                        aria-label={t.answerPlaceholder}
                    />
                    <button type="button" className="sc-btn" disabled={locked || !value.trim()} onClick={() => submit()}>
                        {t.submit}
                    </button>
                </div>
            )}

            {feedback && <p className={`qc-feedback qc-feedback--${feedback.type}`}>{feedback.text}</p>}

            {solutionSteps.length > 0 && (
                <ol className="qc-solution qc-math">
                    {solutionSteps.map((step, i) => <li key={i}>{step}</li>)}
                </ol>
            )}

            {footer}
        </div>
    );
}
