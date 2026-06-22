import { useState } from 'react';
import { format } from '../../i18n/languages.js';
import './QuestionCard.css';

// "5,7,9,11" → ["5","7","9","11"];  "" / null → []
const parseOptions = (s) => (s ? s.split(',').map(o => o.trim()).filter(Boolean) : []);

// Presentational question card — difficulty label, expression (kept LTR even in RTL),
// answer area (multiple-choice grid OR free-text input), and an inline feedback line.
// Purely controlled by props; reusable for any question-answering flow.
//   question  the question object from the API (expression, options, difficultyLevel…)
//   t         questionGameStrings dictionary (Submit / difficulty / answerPlaceholder…)
//   feedback  { type: 'correct'|'wrong'|'failed', text } | null
//   locked    disable inputs (busy or already concluded)
//   onAnswer  (value: string) => void
export function QuestionCard({ question, t, feedback, locked = false, onAnswer }) {
    const [value, setValue] = useState('');
    const options = parseOptions(question?.options);
    const isMultipleChoice = options.length > 0;

    const submit = (v) => {
        const given = (v ?? value).toString().trim();
        if (!given || locked) return;
        onAnswer(given);
    };

    return (
        <div className="qc">
            <div className="qc-diff">{format(t.difficulty, { level: question?.difficultyLevel ?? 1 })}</div>
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
        </div>
    );
}
