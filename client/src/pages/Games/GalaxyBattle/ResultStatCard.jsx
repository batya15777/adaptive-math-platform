// A small info tile inside the battle result modal (topic / streak / hearts).
export const ResultStatCard = ({ icon, label, value }) => (
    <div className="rs-stat">
        <span className="rs-stat-ic" aria-hidden="true">{icon}</span>
        <span className="rs-stat-label">{label}</span>
        <span className="rs-stat-val">{value}</span>
    </div>
);

export default ResultStatCard;
