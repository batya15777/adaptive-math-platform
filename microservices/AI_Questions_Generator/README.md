## AI Questions Generator

FastAPI microservice for generating math questions with a modular agent-style architecture.

**Multilingual Support**: English, Hebrew, and Russian
**Safety Features**: Comprehensive guardrails with child-safe content filtering

### Architecture

- `main.py` — thin FastAPI entrypoint
- `src/settings.py` — environment/config loading
- `src/guardrails.py` — keyword and moderation guardrails
- `src/i18n.py` — internationalization and localization strings
- `src/models.py` — request/response schemas
- `src/tools.py` — safe math calculator utilities
- `src/agents.py` — router, generator, and evaluator logic
- `src/orchestrator.py` — coordinates the full request flow

### Features

- Topic routing between arithmetic and geometry
- Multilingual output (English, Hebrew, Russian)
- Expanded safety guardrails with 5+ keyword categories:
  - Violence, Weapons & Crime
  - Substances (alcohol, drugs, tobacco)
  - Gambling & Financial Fraud
  - Dark Themes & Self-Harm
  - Adult Content & Profanity
  - Political / Malicious Code
- Word-boundary regex matching (prevents false positives like "rob" in "addition")
- Input guardrails with local keyword checks and OpenAI moderation
- Output guardrails for safe text
- Structured Pydantic responses
- Calculator-backed evaluation of generated answers

### Environment Variables

Create a `.env` file in the project root:

```env
OPENAI_API_KEY=your_key_here
OPENAI_MODEL=gpt-4o-mini
OPENAI_MODERATION_MODEL=omni-moderation-latest
```

### Run Locally

```powershell
uv run uvicorn main:app --reload
```

### Example Requests

#### English (Default)

POST `http://127.0.0.1:8000/generate-question`

```json
{
  "topic": "fractions",
  "difficulty": 2,
  "context": "pizza slices",
  "language": "en"
}
```

Expected response (English):
```json
{
  "question_text": "What is 8 + 5?",
  "correct_answer": "13",
  "step_by_step_solution": ["Start with 8", "Apply the operation step by step", "The answer is 13"],
  "difficulty_level": 2,
  "language": "en"
}
```

#### Hebrew (עברית)

```json
{
  "topic": "geometry",
  "difficulty": 2,
  "language": "he"
}
```

Expected response (Hebrew):
```json
{
  "question_text": "לריבוע אורך צלע 4. מה השטח שלו?",
  "correct_answer": "16",
  "step_by_step_solution": ["משתמשים בנוסחת השטח של ריבוע: צלע × צלע", "מציבים את אורך הצלע בנוסחה: 4 * 4", "מחשבים את השטח ומקבלים 16"],
  "difficulty_level": 2,
  "language": "he"
}
```

#### Russian (Русский)

```json
{
  "topic": "fractions",
  "difficulty": 2,
  "language": "ru"
}
```

Expected response (Russian):
```json
{
  "question_text": "Сколько будет 15 + 9?",
  "correct_answer": "24",
  "step_by_step_solution": ["Начинаем с 15", "Выполняем действие шаг за шагом", "Ответ: 24"],
  "difficulty_level": 2,
  "language": "ru"
}
```

### Supported Languages

| Code | Language |
|------|----------|
| `en` | English (default) |
| `he` | Hebrew |
| `ru` | Russian |

### Content Safety

All requests are validated against:

1. **Input Guardrails**:
   - Empty input check
   - Local keyword filtering (5+ categories)
   - OpenAI moderation API for additional protection

2. **Output Guardrails**:
   - Final response safety check
   - Word-boundary matching to prevent false positives
   - Empty response validation

### Health Check

GET `http://127.0.0.1:8000/health`

