# Student Clustering Service

A FastAPI microservice that groups students into **error-pattern cohorts** using
**K-Means (unsupervised learning)**. The Spring Boot backend sends each student's
exercise-attempt history; this service engineers features, clusters the students,
and returns a `user_id → cluster_id` mapping that the backend persists for the
adaptive question generator and the (future) admin dashboard.

Runs on **port 8002** — the AI question generator owns 8000 and the AI tutor chat owns 8001.

---

## Architecture

Routing is kept separate from the ML logic (SOLID / single responsibility):

```
student-clustering-service/
├── main.py                       # FastAPI app: wiring, /health, uvicorn entry
├── app/
│   ├── config.py                 # Settings (port, KMeans random_state, max_k)
│   └── logging_config.py         # Logging setup
├── models/
│   └── schemas.py                # Pydantic request/response models (the API contract)
├── routers/
│   └── clustering.py             # HTTP layer only — validates + delegates, no ML here
└── services/
    ├── feature_engineering.py    # Raw attempts -> numeric feature matrix
    └── clustering_service.py     # K-Means: choose k, fit, summarise clusters
```

---

## Install & run

Dependencies (`scikit-learn`, `numpy`, `fastapi`, `uvicorn`, `pydantic`) are
declared in `pyproject.toml`.

```bash
# from microservices/student-clustering-service
uv sync                                   # install deps into .venv
uv run uvicorn main:app --reload --port 8002
```

Health check:

```bash
curl http://localhost:8002/health
# {"status":"ok","service":"Student Clustering Service"}
```

Interactive API docs: <http://localhost:8002/docs>

---

## API

### `POST /clustering/run`

**Request** (`n_clusters` is optional — omit it to auto-select `k`):

```json
{
  "n_clusters": null,
  "students": [
    {
      "user_id": 1,
      "attempts": [
        {
          "error_pattern": "CONFUSED_SUB_WITH_ADD",
          "question_type": "SIMPLE_SUBTRACTION",
          "difficulty_level": 2,
          "is_correct": false,
          "user_answer": "99",
          "answered_at": "2026-06-20T10:00:00"
        }
      ]
    }
  ]
}
```

**Response**:

```json
{
  "k": 3,
  "silhouette_score": 1.0,
  "assignments": [
    { "user_id": 1, "cluster_id": 0 }
  ],
  "clusters": [
    {
      "cluster_id": 0,
      "size": 5,
      "label": "Needs strong support - mainly CONFUSED_SUB_WITH_ADD",
      "avg_accuracy": 0.0,
      "avg_difficulty": 2.0,
      "top_error_patterns": ["CONFUSED_SUB_WITH_ADD"]
    }
  ]
}
```

Errors: `400` for an empty/invalid request, `500` for unexpected failures.

---

## How it works

### 1. Feature engineering (`services/feature_engineering.py`)

Each student becomes **one row** built from three concatenated blocks:

| Block | Source fields | Encoding |
|-------|---------------|----------|
| **A. Behaviour (numeric)** | `is_correct`, `difficulty_level`, `answered_at`, `user_answer` | accuracy, error rate, avg/max difficulty, attempt count, distinct error count, avg seconds between attempts, empty-answer rate, non-numeric-answer rate → **`StandardScaler`** |
| **B. Error fingerprint** | `error_pattern` (string) | **TF-IDF** over each student's "error document" — distinctive weaknesses are up-weighted, ubiquitous ones down-weighted |
| **C. Failed-topic fingerprint** | `question_type` of *wrong* answers (string) | **TF-IDF** over the topics the student fails |

Blocks B and C are how the **categorical/text fields are vectorised** into numbers
for the distance-based model. Correct answers have `error_pattern = null` and are
handled gracefully (an all-correct cohort simply yields an empty error block).

### 2. K-Means + choosing `k` (`services/clustering_service.py`)

- If the caller passes `n_clusters`, that `k` is used (clamped to the sample size).
- Otherwise `k` is **auto-selected**: K-Means is fit for `k = 2 … min(max_k, n−1)`
  and the `k` with the highest **silhouette score** wins.
- Edge cases are safe: 1–2 students → a single cluster; identical feature rows →
  one cluster; an empty request → `400`.
- `random_state` is fixed for reproducible runs.

Each cluster gets a readable, data-driven `label` (e.g. *"High performers"*,
*"Needs strong support - mainly EMPTY_ANSWER"*) for the admin view.

---

## Backend integration (Spring Boot)

- `MLClusteringService` pulls all attempts via a lightweight projection, groups
  them per student, POSTs to `${ml.clustering.url}/clustering/run` (a `RestTemplate`
  with connect/read timeouts so a down service fails fast), and **upserts** the
  result into the `student_cluster_assignments` table.
- `ml.clustering.url` defaults to `http://localhost:8002` (overridable via the
  `ML_CLUSTERING_URL` env var).
- Trigger: `POST /ml/clustering/run` (optional `?k=` to force a fixed `k`).
- If this service is unreachable, the backend returns `503` with a clear message.
