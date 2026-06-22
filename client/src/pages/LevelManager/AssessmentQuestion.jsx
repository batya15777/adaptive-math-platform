import { useState } from 'react';
import { submitAnswer } from '../../service/progressApi.js';
import { format } from '../../i18n/languages.js';

// "5|7|9|11" → ["5","7","9","11"];  "" / null → []
// Pipe-delimited so an option may contain commas (e.g. "X1=5 , X2=10").
const parseOptions  = (s) => (s ? s.split('|').map(o => o.trim()).filter(Boolean) : []);
// "a = 1\nb = 2" → ["a = 1","b = 2"]
const parseSolution = (s) => (s ? s.split('\n').map(t => t.trim()).filter(Boolean) : []);

export const AssessmentQuestion = ({ question, subSubjectId, subSubjectName, onDone, t }) => {
    const [answer,    setAnswer]    = useState('');
    const [feedback,  setFeedback]  = useState(null); // { type: 'correct'|'wrong', text }
    const [concluded, setConcluded] = useState(false);
    const [loading,   setLoading]   = useState(false);
    const [error,     setError]     = useState('');

    const options  = parseOptions(question.options);
    const steps    = parseSolution(question.solution);
    const isMultipleChoice = options.length > 0;

    const handleAnswer = async (value) => {
        if (concluded || loading) return;
        const given = (value ?? answer).toString().trim();
        if (!given) return;

        setLoading(true);
        setError('');
        try {
            const res = await submitAnswer({
                subSubjectId:     Number(subSubjectId),
                questionId:       question.questionId,
                userAnswer:       given,
                questionType:     subSubjectName || 'ASSESSMENT',
                currentDifficulty: question.difficultyLevel || 1,
                attemptNumber:    1,
            });
            const data = res.data;
            if (data.answerCorrect) {
                setFeedback({ type: 'correct', text: t.feedbackCorrect });
            } else {
                setFeedback({
                    type: 'wrong',
                    text: format(t.feedbackWrong, { answer: question.correctAnswer }),
                });
            }
            setConcluded(true);
        } catch {
            setError(t.errSubmit);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div>
            <h2 style={sectionHeading}>{t.questionHeading}</h2>
            <p style={subtext}>
                {t.questionSubtext}
            </p>

            {error && <p style={errorMsg}>{error}</p>}

            <div style={questionCard}>
                <div style={diffTag}>{format(t.difficulty, { level: question.difficultyLevel })}</div>

                {/* Math stays LTR even when the page is RTL (Hebrew). */}
                <div dir="ltr" style={expression}>{question.expression}</div>

                {isMultipleChoice ? (
                    <div style={optionGrid}>
                        {options.map(opt => (
                            <button
                                key={opt}
                                type="button"
                                disabled={concluded || loading}
                                onClick={() => handleAnswer(opt)}
                                style={optionBtn(concluded)}
                            >
                                {opt}
                            </button>
                        ))}
                    </div>
                ) : (
                    <div style={{ display: 'flex', gap: '10px', marginTop: '12px' }}>
                        <input
                            type="text"
                            value={answer}
                            disabled={concluded || loading}
                            onChange={e => setAnswer(e.target.value)}
                            onKeyDown={e => { if (e.key === 'Enter') handleAnswer(); }}
                            placeholder={t.answerPlaceholder}
                            style={textInput}
                        />
                        <button
                            type="button"
                            disabled={concluded || loading || !answer.trim()}
                            onClick={() => handleAnswer()}
                            style={btn(concluded || !answer.trim() ? '#adb5bd' : '#28a745')}
                        >
                            {t.submit}
                        </button>
                    </div>
                )}

                {feedback && (
                    <p style={{ marginTop: '14px', fontWeight: 'bold', color: feedback.type === 'correct' ? '#28a745' : '#fd7e14' }}>
                        {feedback.text}
                    </p>
                )}

                {/* Show full solution steps after answering */}
                {concluded && steps.length > 0 && (
                    <ol style={solutionBox}>
                        {steps.map((step, i) => (
                            <li key={i} style={{ marginBottom: '4px' }}>{step}</li>
                        ))}
                    </ol>
                )}
            </div>

            {concluded && (
                <button
                    type="button"
                    onClick={onDone}
                    style={{ ...btn('#007bff'), marginTop: '20px', width: '100%', padding: '14px', fontSize: '16px' }}
                >
                    {t.continueToPractice}
                </button>
            )}
        </div>
    );
};

// ── styles ──────────────────────────────────────────────────────────────────

const sectionHeading = { margin: '0 0 6px', fontSize: '20px', color: '#222' };
const subtext        = { margin: '0 0 20px', color: '#777', fontSize: '13px' };
const errorMsg       = { color: '#dc3545', fontSize: '14px', marginBottom: '10px' };

const questionCard = {
    border: '1px solid #ddd', borderRadius: '8px',
    padding: '24px', backgroundColor: '#f9f9f9',
};

const diffTag = { fontSize: '12px', color: '#888', marginBottom: '8px' };

const expression = {
    fontSize: '32px', fontWeight: 'bold', color: '#222',
    textAlign: 'center', padding: '16px 0', letterSpacing: '1px',
};

const optionGrid = { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px', marginTop: '12px' };

const optionBtn = (disabled) => ({
    padding: '14px', fontSize: '18px', fontWeight: 'bold',
    backgroundColor: disabled ? '#e9ecef' : '#fff',
    color: '#333', border: '2px solid #007bff', borderRadius: '6px',
    cursor: disabled ? 'default' : 'pointer',
});

const textInput = {
    flex: 1, padding: '12px', fontSize: '18px',
    border: '1px solid #ccc', borderRadius: '4px',
    boxSizing: 'border-box',
};

const solutionBox = {
    marginTop: '14px', padding: '12px 12px 12px 28px',
    backgroundColor: '#eef6ff', border: '1px solid #cfe2ff',
    borderRadius: '6px', color: '#333',
};

const btn = (bg) => ({
    padding: '10px 16px', backgroundColor: bg, color: '#fff',
    border: 'none', borderRadius: '4px', cursor: 'pointer',
    fontSize: '14px', fontWeight: 'bold',
});
