// Metric card used in both AdminDashboard and AdminMLGroups.
// Styled via admin.css tokens — theme-aware via CSS variables.
export const StatCard = ({ label, value, icon = "📊", color = "#7C4DFF" }) => (
    <div className="adm-stat-card">
        <div className="adm-stat-top-bar" style={{ background: color }} />
        <div className="adm-stat-icon">{icon}</div>
        <div className="adm-stat-value">{value}</div>
        <div className="adm-stat-label">{label}</div>
    </div>
);
