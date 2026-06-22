export const BroadcastAlert = ({ messages, onDismiss }) => {
    if (!messages || messages.length === 0) return null;

    return (
        <div style={overlay}>
            {messages.map((msg, i) => (
                <div key={i} style={card}>
                    <span style={text}>{msg}</span>
                    <button style={closeBtn} onClick={() => onDismiss(i)} aria-label="Dismiss">✕</button>
                </div>
            ))}
        </div>
    );
};

const overlay = {
    position: "fixed",
    top: 16,
    left: "50%",
    transform: "translateX(-50%)",
    zIndex: 9999,
    display: "flex",
    flexDirection: "column",
    gap: 8,
    width: "min(480px, 90vw)",
    pointerEvents: "none",
};

const card = {
    display: "flex",
    alignItems: "center",
    justifyContent: "space-between",
    gap: 12,
    padding: "12px 16px",
    background: "#1a73e8",
    color: "#fff",
    borderRadius: 8,
    boxShadow: "0 4px 16px rgba(0,0,0,0.25)",
    fontSize: 15,
    fontFamily: "Arial, sans-serif",
    pointerEvents: "auto",
};

const text = { flex: 1 };

const closeBtn = {
    background: "transparent",
    border: "none",
    color: "#fff",
    fontSize: 18,
    cursor: "pointer",
    padding: "0 4px",
    lineHeight: 1,
    flexShrink: 0,
};
