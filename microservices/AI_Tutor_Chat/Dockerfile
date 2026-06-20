# syntax=docker/dockerfile:1

FROM python:3.12-slim

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_DISABLE_PIP_VERSION_CHECK=1

WORKDIR /app

COPY pyproject.toml ./
RUN pip install --no-cache-dir \
    fastapi==0.136.3 \
    openai==2.41.0 \
    pydantic==2.13.4 \
    python-dotenv==1.2.2 \
    uvicorn==0.49.0

COPY main.py ./
COPY src ./src

EXPOSE 8001

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8001"]
