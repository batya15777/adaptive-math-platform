## AI Questions Generator

FastAPI microservice for generating creative, themed educational questions powered by OpenAI.

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
[Input Guardrails]  — keyword blocklist + OpenAI Moderation API
  │
  ▼
[QuestionAgent.generate()]  — GPT call: themed, personalised question
  │
  ▼
[QuestionAgent.evaluate()]  — second GPT call (temp=0): independent answer verification
  │   (auto-retries once if answer is wrong)
  ▼
[Output Guardrails]  — final safety pass on question text
  │
  ▼
Response
```

---

### Environment Variables

Create a `.env` file in the `microservices/AI_Questions_Generator/` directory:

```env
OPENAI_API_KEY=your_key_here
OPENAI_MODEL=gpt-4o-mini
OPENAI_MODERATION_MODEL=omni-moderation-latest
```

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
  "user_info": {
    "name": "Gabi",
    "age": 9
  },
  "language": "en"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `topic` | string | ✅ | Subject being tested (fractions, history, geometry, …) |
| `theme` | string | ✅ | Narrative theme / creative setting (pirates, space, …) |
| `difficulty` | int 1–10 | ✅ | 1 = kindergarten-easy, 10 = advanced high-school |
| `user_info.name` | string | ✅ | Student's first name (used for personalisation) |
| `user_info.age` | int 4–18 | ✅ | Student's age in years |
| `user_info.*` | any | ➕ | Any extra fields (grade, learning_style, …) are forwarded to the LLM |
| `language` | `en`/`he`/`ru` | ✅ | Output language |

**Response** (`200 OK`):

```json
{
  "question_text": "On a pirate ship there are 10 pirates, 2 parrots, and the captain. How many apples do they need for a day if everyone eats 2 each?",
  "correct_answer": "26",
  "step_by_step_solution": [
    "Count everyone on the ship: 10 pirates + 2 parrots + 1 captain = 13 total.",
    "Each one eats 2 apples per day: 13 × 2 = 26.",
    "So the ship needs 26 apples for the day."
  ],
  "difficulty_level": 5,
  "language": "en"
}
```

**Error responses**:

| Status | Cause |
|--------|-------|
| `400` | Input blocked by guardrails, or answer verification failed after 2 attempts |
| `503` | `OPENAI_API_KEY` is missing |
| `500` | Unexpected server error |

---

### More Examples

#### Hebrew — geometry + space theme

```json
{
  "topic": "geometry",
  "theme": "space station",
  "difficulty": 4,
  "user_info": { "name": "נועה", "age": 10 },
  "language": "he"
}
```

#### Russian — history + ancient Egypt

```json
{
  "topic": "ancient history",
  "theme": "ancient egypt",
  "difficulty": 6,
  "user_info": { "name": "Миша", "age": 12 },
  "language": "ru"
}
```

#### English — algebra + wizards, with extra student context

```json
{
  "topic": "linear equations",
  "theme": "wizard school",
  "difficulty": 8,
  "user_info": {
    "name": "Sam",
    "age": 14,
    "grade": "8th",
    "learning_style": "visual"
  },
  "language": "en"
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
