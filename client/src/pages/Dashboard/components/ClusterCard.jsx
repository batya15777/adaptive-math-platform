import { Card, prettifyError, prettifyClusterLabel } from './DashboardPrimitives.jsx';
import { format } from '../../../i18n/languages.js';

// "My learning group" — the student's ML cohort as pure statistical info (no action
// button). Shows the group name, match %, a plain-language explanation, and the shared
// focus areas. Pure: receives the strings dictionary `t`.
export const ClusterCard = ({ cluster, t }) => {
    if (!cluster || !cluster.assigned) {
        return (
            <Card title={t.clusterTitle}>
                <p style={{ color: 'var(--mg-tm)', margin: 0, fontSize: 14 }}>{t.clusterPending}</p>
            </Card>
        );
    }

    const matchPct = cluster.avgAccuracy != null ? Math.round(cluster.avgAccuracy * 100) : null;
    const patterns = cluster.topErrorPatterns || [];

    return (
        <Card title={t.clusterTitle}>
            <div className="stat-group">
                <div className="stat-group-row">
                    <span className="stat-group-orb" aria-hidden="true" />
                    <div style={{ flex: 1, minWidth: 160 }}>
                        <div className="stat-group-label">{t.youreInTheGroup}</div>
                        <div className="stat-group-name">{prettifyClusterLabel(cluster.label, t)}</div>
                        {matchPct != null && (
                            <div className="stat-group-match">{format(t.clusterMatch, { percent: matchPct })}</div>
                        )}
                    </div>
                </div>

                <p className="stat-group-explain">{t.clusterExplain}</p>

                {patterns.length > 0 && (
                    <div>
                        <div className="stat-group-focus-title">{t.practicingTogether}</div>
                        <div className="stat-chips">
                            {patterns.map((p) => <span className="stat-chip" key={p}>{prettifyError(p, t)}</span>)}
                        </div>
                    </div>
                )}
            </div>
        </Card>
    );
};
