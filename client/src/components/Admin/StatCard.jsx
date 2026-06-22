// Small presentational metric card for the admin dashboard. Props only.
export const StatCard = ({ label, value }) => (
    <div style={card}>
        <div style={cardValue}>{value}</div>
        <div style={cardLabel}>{label}</div>
    </div>
);

const card = {
    flex: "1 1 140px", minWidth: 140, background: "#fff", border: "1px solid #eee",
    borderRadius: 12, padding: "16px 18px", boxShadow: "0 1px 3px rgba(0,0,0,0.06)",
};
const cardValue = { fontSize: 28, fontWeight: 800, color: "#aa3bff" };
const cardLabel = { marginTop: 4, fontSize: 13, color: "#6c757d" };
