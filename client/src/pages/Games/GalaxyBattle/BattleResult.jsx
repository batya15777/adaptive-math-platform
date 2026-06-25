import Confetti from 'react-confetti';
import { ResultStatCard } from './ResultStatCard.jsx';

// Battle end modal — celebratory, neon, game-like (not a plain alert). Win → confetti + reward;
// lose → encouragement. Shows the final topic / streak / hearts. Presentational only.
export const BattleResult = ({ won, topicName, bs, onAnotherBattle, onBackToGames, onBackHome, onPractice, t, g }) => {
    const streak = bs?.streak ?? 0;
    const lives = bs?.playerLives ?? 0;

    return (
        <div className="gb-overlay gb-result-overlay" role="dialog" aria-label={won ? t.winTitle : t.loseTitle}>
            {won && (
                <Confetti width={window.innerWidth} height={window.innerHeight}
                    recycle={false} numberOfPieces={420} gravity={0.22}
                    style={{ position: 'fixed', inset: 0, zIndex: 50, pointerEvents: 'none' }} />
            )}
            <div className="gb-result">
                <div className="gb-result-crest" aria-hidden="true">
                    <span className="gb-result-wing gb-result-wing--l">🪽</span>
                    <span className="gb-result-shield">{won ? '🏆' : '💥'}</span>
                    <span className="gb-result-wing gb-result-wing--r">🪽</span>
                </div>

                <h2 className="gb-result-title">{won ? t.winTitle : t.loseTitle}</h2>
                <p className="gb-result-sub">{won ? t.winSub : g(t.loseSub)}</p>

                <div className="gb-result-stats">
                    <ResultStatCard icon="🪐" label={t.resultTopic} value={topicName || '—'} />
                    <ResultStatCard icon="🔥" label={t.resultStreak} value={streak} />
                    <ResultStatCard icon="❤️" label={t.resultHearts} value={`${lives}/5`} />
                </div>

                {won && <div className="gb-result-reward">{t.winBadge}</div>}

                <div className="gb-result-actions">
                    <button type="button" className="sc-btn gb-result-cta" onClick={onAnotherBattle}>⚔️ {t.anotherBattle}</button>
                    {won
                        ? <button type="button" className="sc-btn sc-btn--ghost" onClick={onBackToGames}>{t.backToGames}</button>
                        : <button type="button" className="sc-btn sc-btn--ghost" onClick={onPractice}>✏️ {t.practiceTopic}</button>}
                    <button type="button" className="sc-btn sc-btn--ghost" onClick={onBackHome}>🏠 {t.backHome}</button>
                </div>
            </div>
        </div>
    );
};

export default BattleResult;
