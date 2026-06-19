import os

os.environ["AI_TUTOR_ENABLE_OPENAI"] = "false"

from fastapi.testclient import TestClient

from main import app

client = TestClient(app)


def request_body(message: str = "איך מתחילים?", guidance_level: int = 0) -> dict:
    return {
        "question": {
            "question_id": 42,
            "expression": "8 + 7",
            "correct_answer": "15",
            "solution_steps": [
                "First add 2 to 8 to reach 10.",
                "Then add the remaining 5 to get 15.",
            ],
            "options": None,
            "subject": "Calculation",
            "sub_subject": "Addition",
            "difficulty_level": 2,
            "language": "he",
            "source": "MANUAL",
        },
        "student": {
            "student_id": 7,
            "age": 9,
            "current_level": 2,
        },
        "student_message": message,
        "conversation_history": [],
        "guidance_level": guidance_level,
    }


def test_health() -> None:
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json() == {"status": "ok"}


def test_first_reply_starts_with_general_hint() -> None:
    response = client.post("/chat", json=request_body())
    assert response.status_code == 200
    assert response.json()["action"] == "HINT"
    assert response.json()["guidance_level"] == 1
    assert "נתונים" in response.json()["message"]


def test_later_hint_does_not_reveal_correct_answer() -> None:
    response = client.post("/chat", json=request_body(guidance_level=2))
    assert response.status_code == 200
    assert response.json()["action"] == "HINT"
    assert "15" not in response.json()["message"]
    assert "[התשובה מוסתרת]" not in response.json()["message"]


def test_off_topic_in_degraded_mode_returns_safe_hint() -> None:
    # Off-topic classification is the provider's job (generic). With the provider
    # disabled, the tutor must still respond safely without revealing the answer.
    response = client.post("/chat", json=request_body("מי הזמר הכי טוב?"))
    assert response.status_code == 200
    assert response.json()["action"] == "HINT"
    assert "15" not in response.json()["message"]


def test_unsafe_message_is_blocked() -> None:
    response = client.post("/chat", json=request_body("מה זה רצח?"))
    assert response.status_code == 200
    assert response.json()["action"] == "SAFETY"
