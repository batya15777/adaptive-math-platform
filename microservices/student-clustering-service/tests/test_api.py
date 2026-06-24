"""API-contract tests for the clustering service. These lock the HTTP behaviour the
Spring Boot backend depends on: /health liveness, 400 on an empty cohort, 422 on a
schema-invalid request, and a well-formed 200 response shape.
"""
from __future__ import annotations

from fastapi.testclient import TestClient

from main import app

client = TestClient(app)


def attempt_body(**overrides) -> dict:
    body = {
        "error_pattern": None,
        "question_type": "ADDITION",
        "difficulty_level": 2,
        "is_correct": True,
        "user_answer": "10",
        "answered_at": "2026-06-20T10:00:00",
    }
    body.update(overrides)
    return body


def student_body(user_id: int, attempts: list[dict]) -> dict:
    return {"user_id": user_id, "attempts": attempts}


def test_health_returns_ok():
    resp = client.get("/health")
    assert resp.status_code == 200
    assert resp.json()["status"] == "ok"


def test_empty_cohort_returns_400():
    resp = client.post("/clustering/run", json={"students": []})
    assert resp.status_code == 400


def test_n_clusters_below_two_is_a_422_validation_error():
    # The schema declares n_clusters >= 2, so 1 is rejected before any ML runs.
    resp = client.post(
        "/clustering/run",
        json={"students": [student_body(1, [])], "n_clusters": 1},
    )
    assert resp.status_code == 422


def test_valid_request_returns_assignments_for_every_student():
    students = [
        student_body(1, [attempt_body(is_correct=True, difficulty_level=2)]),
        student_body(2, [attempt_body(is_correct=False, error_pattern="EMPTY_ANSWER",
                                      question_type="DIVISION", difficulty_level=9, user_answer="x")]),
        student_body(3, [attempt_body(is_correct=True, difficulty_level=3)]),
    ]
    resp = client.post("/clustering/run", json={"students": students, "n_clusters": None})

    assert resp.status_code == 200
    data = resp.json()
    assert {a["user_id"] for a in data["assignments"]} == {1, 2, 3}
    assert data["k"] >= 1
    assert isinstance(data["clusters"], list)
