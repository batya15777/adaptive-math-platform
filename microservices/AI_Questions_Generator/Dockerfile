# syntax=docker/dockerfile:1

FROM python:3.12-slim

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_DISABLE_PIP_VERSION_CHECK=1 \
    PORT=8000

WORKDIR /app

# Install Python dependencies first for better Docker layer caching.
COPY pyproject.toml ./
RUN pip install --no-cache-dir \
    fastapi==0.136.3 \
    openai==2.41.0 \
    pydantic==2.13.4 \
    python-dotenv==1.2.2 \
    uvicorn==0.49.0

# Copy application source.
COPY main.py ./
COPY src ./src
COPY README.md ./

EXPOSE 8000

# Render provides $PORT; bind 0.0.0.0 so the service is reachable. Shell form so
# ${PORT} is expanded at runtime (falls back to 8000 for local `docker run`).
CMD ["sh", "-c", "uvicorn main:app --host 0.0.0.0 --port ${PORT}"]
