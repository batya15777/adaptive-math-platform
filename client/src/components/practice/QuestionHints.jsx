import { useState } from 'react';
import { parseSolution } from '../../utils/questionFormat.js';
import './QuestionHints.css';

// Staged hints (💡 רמז) + full guided solution (פתרון מודרך), extracted verbatim from
// QuestionGame so the SAME logic is reused by both the regular practice and the daily
// practice — no duplicate implementation. Rendered inside QuestionCard's `footer` slot,
// with the card's own `showSolution` disabled so this component is the single source of
// the worked steps.
//
//   question          the question object — its `solution` is parsed into steps here
//   t                 questionGameStrings dictionary (hint / showSolution labels)
//   concluded         the question is finished (correct / failed) — disables the buttons
//   loading           a request is in flight — disables the solution button
//   showFullSolution  reveal ALL steps at once (parent sets this when the question is
//                     failed or the student asked for the full solution)
//   onRevealSolution  called when the student clicks "show full solution"
//   hideActions       hide the two buttons (e.g. bonus questions) while still letting the
//                     parent reveal the full solution via `showFullSolution`
//
// Keep the per-question hint state fresh by giving this component `key={question.questionId}`
// in the parent, so it remounts (revealedHints → 0) on every new question.
export function QuestionHints({
    question, t, concluded = false, loading = false,
    showFullSolution = false, onRevealSolution, hideActions = false,
}) {
    const [revealedHints, setRevealedHints] = useState(0);
    const steps = question ? parseSolution(question.solution) : [];

    // Hint is disabled with ≤1 step, or once only the final step is left unrevealed
    // (the last step is the answer — that comes from the full solution, not a hint).
    const hintDisabled = steps.length <= 1 || revealedHints >= steps.length - 1 || concluded;

    return (
        <>
            {!hideActions && (
                <div className="qh-actions">
                    <button type="button" className="sc-btn sc-btn--ghost"
                        onClick={() => setRevealedHints(n => n + 1)} disabled={hintDisabled}>
                        {t.hint}
                    </button>
                    <button type="button" className="sc-btn sc-btn--ghost"
                        onClick={onRevealSolution} disabled={concluded || loading}>
                        {t.showSolution}
                    </button>
                </div>
            )}

            {(revealedHints > 0 || showFullSolution) && steps.length > 0 && (
                <ol className="qc-solution qc-math">
                    {(showFullSolution ? steps : steps.slice(0, revealedHints)).map((step, i) => (
                        <li key={i}>{step}</li>
                    ))}
                </ol>
            )}
        </>
    );
}
