import { parseOptions } from '../../../utils/questionFormat.js';

const ATTACK_ICONS = ['⚡', '🔥', '💫', '🚀'];

// Game-styled question panel (NOT the practice exercise card). Same data + onAnswer contract —
// only the look changes: a command bar + neon "attack" buttons. Math stays LTR, Hebrew prose RTL.
export const BattleQuestionPanel = ({ question, feedback, locked, onAnswer, t }) => {
    const expr = question?.expression ?? '';
    const isWord = (expr.match(/\p{L}/gu) || []).length >= 12; // word problem vs bare math
    const options = parseOptions(question?.options);
    const picked = feedback?.picked != null ? String(feedback.picked).trim() : null;

    return (
        <div className="gb-qpanel">
            <div className="gb-qpanel-head">
                <span aria-hidden="true">⚔️</span> {t.attackPrompt}
            </div>
            <div className={'gb-qpanel-expr' + (isWord ? ' is-word' : '')} dir={isWord ? 'auto' : 'ltr'}>
                {expr}
            </div>
            <div className="gb-qpanel-opts">
                {options.map((opt, i) => {
                    const isPicked = picked != null && String(opt).trim() === picked;
                    const fxClass = isPicked ? (feedback.type === 'correct' ? ' is-correct' : ' is-wrong') : '';
                    return (
                        <button
                            key={`${opt}-${i}`}
                            type="button"
                            className={'gb-cmd' + fxClass}
                            disabled={locked}
                            onClick={() => onAnswer(opt)}
                        >
                            <span className="gb-cmd-ic" aria-hidden="true">{ATTACK_ICONS[i % ATTACK_ICONS.length]}</span>
                            <span className="gb-cmd-text" dir="ltr">{opt}</span>
                        </button>
                    );
                })}
            </div>
        </div>
    );
};

export default BattleQuestionPanel;
