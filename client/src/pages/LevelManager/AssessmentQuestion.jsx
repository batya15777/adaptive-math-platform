import { useState } from 'react';
import { submitAnswer, getNextQuestion } from '../../service/progressApi.js';
import { format } from '../../i18n/languages.js';

// Number of placement questions (matches the sketch's "1 of 4").
const TOTAL = 4;

// "5,7,9,11" → ["5","7","9","11"];  "" / null → []
const parseOptions = (s) => (s ? String(s).split(',').map(o => o.trim()).filter(Boolean) : []);

// Forward-only placement quiz: pick an answer → Continue submits it and loads the next
// adaptive question. Each answer feeds the existing leveling logic (submitAnswer). After
// the last question we're done. Back returns to the survey step.
export const AssessmentQuestion = ({ question, subSubjectId, subSubjectName, language, onBack, onDone, t }) => {
    const [questions, setQuestions] = useState([question]);
    const [index,     setIndex]     = useState(0);
    const [selected,  setSelected]  = useState('');
    const [text,      setText]      = useState('');
    const [loading,   setLoading]   = useState(false);
    const [error,     setError]     = useState('');

    const current = questions[index];
    const options = parseOptions(current.options);
    const isMultipleChoice = options.length > 0;
    const answer = (isMultipleChoice ? selected : text).trim();
    const canContinue = !!answer && !loading;
    const isLast = index >= TOTAL - 1;

    const handleContinue = async () => {
        if (!canContinue) return;
        setLoading(true);
        setError('');
        try {
            await submitAnswer({
                subSubjectId:      Number(subSubjectId),
                questionId:        current.questionId,
                userAnswer:        answer,
                questionType:      subSubjectName || 'ASSESSMENT',
                currentDifficulty: current.difficultyLevel || 1,
                attemptNumber:     1,
            });

            if (isLast) {
                onDone();
                return;
            }
            // Load the next (adaptive) placement question.
            const res = await getNextQuestion(Number(subSubjectId), language);
            setQuestions(prev => [...prev, res.data]);
            setIndex(i => i + 1);
            setSelected('');
            setText('');
        } catch {
            // Graceful: never block the user on a placement quiz — just finish.
            onDone();
        } finally {
            setLoading(false);
        }
    };

    return (
        <div>
            <div className="lvl-head">
                <h2>{t.questionHeading}</h2>
                <p>{t.questionSubtext}</p>
            </div>
            <div className="lvl-divider" />

            {error && <p className="lvl-error">{error}</p>}

            <div className="lvl-qcard">
                <p className="lvl-counter">{format(t.questionCounter, { current: index + 1, total: TOTAL })}</p>

                {/* Math stays LTR even when the page is RTL. */}
                <div className="lvl-expr" dir="ltr">{current.expression}</div>

                {isMultipleChoice ? (
                    <div className="lvl-opts">
                        {options.map(opt => (
                            <button
                                key={opt}
                                type="button"
                                disabled={loading}
                                className={'lvl-opt' + (selected === opt ? ' is-sel' : '')}
                                onClick={() => setSelected(opt)}
                            >
                                {opt}
                            </button>
                        ))}
                    </div>
                ) : (
                    <div className="lvl-textrow">
                        <input
                            className="lvl-textinput"
                            dir="ltr"
                            type="text"
                            value={text}
                            disabled={loading}
                            placeholder={t.answerPlaceholder}
                            onChange={e => setText(e.target.value)}
                            onKeyDown={e => { if (e.key === 'Enter') handleContinue(); }}
                        />
                    </div>
                )}

                <p className="lvl-noWrong">{t.noWrongAnswer}</p>
            </div>

            <div className="lvl-actions">
                <button type="button" className="mg-cta" onClick={handleContinue} disabled={!canContinue}>
                    {loading ? t.loading : isLast ? t.continueToPractice : <>{t.continueBtn} ✨</>}
                </button>
                <button type="button" className="lvl-back" onClick={onBack} disabled={loading}>
                    {t.backBtn}
                </button>
            </div>
        </div>
    );
};
