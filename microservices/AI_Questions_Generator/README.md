## AI Questions Generator

FastAPI microservice for generating creative, themed educational questions powered by OpenAI.

Questions are generated as **reusable templates** — the student's name appears as the literal
placeholder `NAME` in every question text. The frontend replaces `NAME` with the actual student's
name at render time.

**Subjects**: Any topic — fractions, history, geometry, algebra, biology, and more  
**Themes**: Any narrative setting — pirates, space, dinosaurs, wizards, sports, and more  
**Multilingual**: English, Hebrew, Russian  
**Safety**: Layered guardrails (local keyword filter + OpenAI Moderation API)

---

### Architecture

```
main.py                  — Thin FastAPI entrypoint
src/settings.py          — Environment / config loading
src/models.py            — Request & response Pydantic schemas
src/security.py          — API key authentication dependency
src/guardrails.py        — Input + output safety guardrails
src/i18n.py              — Localised error messages & language names
src/agents.py            — QuestionAgent: LLM generator + LLM evaluator
src/orchestrator.py      — Coordinates the full request pipeline
src/tools.py             — Safe AST-based math expression evaluator (utility)
```

---

### How It Works

```
Request
  │
  ▼
[API Key Check]  — 403 if key is configured and header is missing/wrong
  │
  ▼
[Input Guardrails]  — keyword blocklist + OpenAI Moderation API
  │
  ▼
[QuestionAgent.generate()]  — GPT call: themed question with NAME placeholder
  │
  ▼
[QuestionAgent.evaluate()]  — second GPT call (temp=0): independent answer verification
  │   (auto-retries once if answer is wrong)
  ▼
[Output Guardrails]  — final safety pass on question text
  │
  ▼
Response  (reusable template — NAME placeholder in question_text)
```

The Java server caches generated questions in the `generated_questions` table keyed by
`(sub_subject_id, language, difficulty_level, multiple_choice)`. Subsequent requests for
the same key return a cached question without calling this service.

---

### Environment Variables

Create a `.env` file in the `microservices/AI_Questions_Generator/` directory:

```env
OPENAI_API_KEY=your_key_here
OPENAI_MODEL=gpt-4o-mini
OPENAI_MODERATION_MODEL=omni-moderation-latest

# Optional — see Security section below
API_KEY=your-shared-secret
```

---

### Security

The `/generate-question` endpoint can be protected with a pre-shared API key so that only
the main Java server can call it:

```env
API_KEY=your-secret-key-here
```

When `API_KEY` is set, every request to `/generate-question` must include:

```
X-API-Key: your-secret-key-here
```

Requests with a missing or incorrect key receive `403 Forbidden`.

If `API_KEY` is **not** set (or is absent from the environment), the endpoint is open —
suitable for local development.

The `/health` endpoint is always open regardless of this setting.

**Java server**: set `QUESTIONS_AI_API_KEY` to the same value in the Java environment
(maps to `questions.ai.api-key` in `application.properties`). The server includes the
header automatically on every call.

---

### Run Locally

```powershell
uv run uvicorn main:app --reload
```

---

### API

#### `POST /generate-question`

**Request body**:

```json
{
  "topic": "fractions",
  "theme": "pirates",
  "difficulty": 5,
  "language": "en"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `topic` | string | ✅ | Subject being tested (fractions, history, geometry, …) |
| `theme` | string | ✅ | Narrative theme / creative setting (pirates, space, …) |
| `difficulty` | int 1–10 | ✅ (or use `difficulty_band`) | 1 = kindergarten-easy, 10 = advanced high-school. Takes precedence over `difficulty_band`. |
| `difficulty_band` | `"easy"` / `"medium"` / `"hard"` | ✅ (or use `difficulty`) | Coarse alternative. Maps to: easy→2, medium→5, hard→8. Ignored when `difficulty` is provided. |
| `language` | `en`/`he`/`ru` | ✅ | Output language |
| `multiple_choice` | bool | ➕ | Default `false`. When `true`, response includes 4 answer options. |
| `learning_profile` | object | ➕ | Cluster-derived adaptation hints (see below). |

At least one of `difficulty` or `difficulty_band` must be provided.

**`learning_profile` fields** (all optional):

| Field | Type | Description |
|-------|------|-------------|
| `cluster_label` | string | Human label of the student's ML cohort |
| `focus_skill` | string | Skill/weakness to gently target in the question |
| `mastery` | `"struggling"` / `"developing"` / `"strong"` | Calibrates challenge level |

**Response** (`200 OK`):

```json
{
  "question_text": "NAME, on a pirate ship there are 10 pirates and a captain. If everyone eats 2 apples a day, how many apples does the ship need?",
  "correct_answer": "22",
  "step_by_step_solution": [
    "Count everyone on the ship: 10 pirates + 1 captain = 11 total.",
    "Each person eats 2 apples: 11 × 2 = 22.",
    "The ship needs 22 apples."
  ],
  "difficulty_level": 5,
  "language": "en"
}
```

The `question_text` always contains the literal word `NAME` — replace it with the student's
actual name before displaying.

**Error responses**:

| Status | Cause |
|--------|-------|
| `400` | Input blocked by guardrails, or answer verification failed after 2 attempts |
| `403` | Missing or invalid `X-API-Key` header (only when `API_KEY` is configured) |
| `503` | `OPENAI_API_KEY` is missing |
| `500` | Unexpected server error |

---

### More Examples

#### Band-only difficulty (no numeric value needed)

```json
{
  "topic": "multiplication",
  "theme": "dinosaurs",
  "difficulty_band": "easy",
  "language": "en"
}
```

→ Generates a question at difficulty 2 (easy midpoint).

#### Both `difficulty` and `difficulty_band` — explicit wins

```json
{
  "topic": "algebra",
  "theme": "space",
  "difficulty": 7,
  "difficulty_band": "hard",
  "language": "en"
}
```

→ Generates at difficulty 7 (`difficulty` takes precedence); the band acts as a supplementary
  hint to the LLM.

#### Hebrew — geometry + space theme

```json
{
  "topic": "geometry",
  "theme": "space station",
  "difficulty": 4,
  "language": "he"
}
```

#### Russian — history + ancient Egypt

```json
{
  "topic": "ancient history",
  "theme": "ancient egypt",
  "difficulty": 6,
  "language": "ru"
}
```

#### English — multiple choice with cluster context

```json
{
  "topic": "linear equations",
  "theme": "wizard school",
  "difficulty": 8,
  "multiple_choice": true,
  "language": "en",
  "learning_profile": {
    "cluster_label": "struggling-algebra",
    "focus_skill": "isolating variables",
    "mastery": "struggling"
  }
}
```

---

### Supported Languages

| Code | Language |
|------|----------|
| `en` | English (default) |
| `he` | Hebrew (עברית) |
| `ru` | Russian (Русский) |

---

### Content Safety

All requests pass through two layers of safety checks:

**Input Guardrails** (before any LLM call):
- Local keyword blocklist (6 categories: violence, substances, gambling, dark themes, adult content, policy)
- Word-boundary regex matching — prevents false positives (e.g. "rob" in "problem")
- Hebrew prefix variants (e.g. "בסמים" → matches "סמים")
- OpenAI Moderation API for additional coverage

**Output Guardrails** (after generation):
- Final keyword scan on the generated question text

---

### Health Check

```
GET /health
→ { "status": "ok" }
```

Always open — no API key required.
