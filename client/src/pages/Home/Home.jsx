
import TutorChat from "../../components/TutorChat/TutorChat.jsx";

export const Home = () => {
    return (
        <main style={{
            display: "grid",
            gridTemplateColumns: "minmax(0, 1fr) 380px",
            gap: "24px",
            width: "min(1100px, calc(100% - 32px))",
            margin: "32px auto",
            alignItems: "start",
            direction: "rtl"
        }}>
            <section style={{
                background: "#ffffff",
                border: "1px solid #e5e7eb",
                borderRadius: "18px",
                padding: "28px",
                boxShadow: "0 14px 35px rgba(15, 23, 42, 0.08)"
            }}>
                <p style={{ margin: "0 0 10px", color: "#2563eb", fontWeight: 700 }}>
                    בדיקת צ׳אט זמנית
                </p>
                <h1 style={{ margin: "0 0 12px", color: "#111827" }}>
                    כמה הם 8 ועוד 7?
                </h1>
                <p style={{ color: "#4b5563" }}>
                    זה אזור בדיקה זמני כדי לוודא שהצ׳אט עובד אחרי הרשמה והתחברות.
                </p>
            </section>

            <TutorChat questionId={1} />
        </main>
    );
}
