# syntax=docker/dockerfile:1

FROM python:3.12-slim

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_DISABLE_PIP_VERSION_CHECK=1 \
    PORT=8002

WORKDIR /app

# Install dependencies first for better layer caching. Versions mirror pyproject.toml;
# quotes are required so the shell does not treat ">=" as a redirection.
COPY pyproject.toml ./
RUN pip install --no-cache-dir \
    "fastapi>=0.138.0" \
    "uvicorn>=0.49.0" \
    "pydantic>=2.13.0" \
    "scikit-learn>=1.9.0" \
    "numpy>=2.0.0"

# Copy application source (routing + ML packages).
COPY main.py ./
COPY app ./app
COPY models ./models
COPY routers ./routers
COPY services ./services

EXPOSE 8002

# Render provides $PORT; bind 0.0.0.0 so the service is reachable. Shell form so
# ${PORT} is expanded at runtime (falls back to 8002 for local `docker run`).
CMD ["sh", "-c", "uvicorn main:app --host 0.0.0.0 --port ${PORT}"]
