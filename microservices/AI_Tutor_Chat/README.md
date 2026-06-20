# AI Tutor Chat

Independent FastAPI microservice that guides a student through the current
exercise without immediately revealing the correct answer.

## Current scope

- Receives trusted exercise and student context from Spring Boot.
- Returns progressively more specific hints.
- Redirects clearly unrelated questions back to the exercise.
- Blocks unsafe requests.
- Supports Hebrew, English, and Russian.
- Does not access the database directly.
- Calls OpenAI when `OPENAI_API_KEY` is configured.
- Falls back to local hints if OpenAI is unavailable.

## Architecture

```text
React -> Spring Boot -> AI Tutor Chat -> future AI provider
```

Spring Boot is responsible for authentication, loading the question by ID, and
verifying that the student may access it.

## Run locally

```bash
uv sync --dev
uv run uvicorn main:app --reload --port 8001
```

API documentation is available at `http://localhost:8001/docs`.

## Environment variables

Create a local `.env` file:

```env
OPENAI_API_KEY=your_key_here
OPENAI_MODEL=gpt-4o-mini
AI_TUTOR_ENABLE_OPENAI=true
```

The `.env` file is ignored by Git.

## Test

```bash
uv run pytest
```

## API

### `GET /health`

Returns:

```json
{"status": "ok"}
```

### `POST /chat`

Example request:

```json
{
  "question": {
    "question_id": 42,
    "expression": "8 + 7",
    "correct_answer": "15",
    "solution_steps": [
      "First add 2 to 8 to reach 10.",
      "Then add the remaining 5."
    ],
    "options": null,
    "subject": "Calculation",
    "sub_subject": "Addition",
    "difficulty_level": 2,
    "language": "he",
    "source": "MANUAL"
  },
  "student": {
    "student_id": 7,
    "age": 9,
    "current_level": 2
  },
  "student_message": "איך מתחילים?",
  "conversation_history": [],
  "guidance_level": 0
}
```

Example response:

```json
{
  "message": "בואי נתחיל בצעד קטן: אילו נתונים חשובים מופיעים בשאלה?",
  "guidance_level": 1,
  "action": "HINT"
}
```
