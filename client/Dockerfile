# syntax=docker/dockerfile:1

# ----------------------------------------------------------------------------
# Stage 1 — build the static assets with Vite
# ----------------------------------------------------------------------------
FROM node:22-alpine AS build
WORKDIR /app

# Install dependencies first so this layer is cached unless the lockfile changes.
COPY package.json package-lock.json ./
RUN npm ci

# Build-time backend URL. Vite inlines VITE_* variables into the bundle at build
# time. NOTE: this only takes effect once src/service/api.js reads
# import.meta.env.VITE_API_BASE_URL (see the deployment note) — otherwise the
# bundle keeps the hard-coded http://localhost:8080.
ARG VITE_API_BASE_URL
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL

COPY . .
RUN npm run build

# ----------------------------------------------------------------------------
# Stage 2 — serve the built files with nginx, bound to Render's $PORT
# ----------------------------------------------------------------------------
FROM nginx:1.27-alpine AS runtime

# Local default; Render overrides $PORT at runtime.
ENV PORT=8080

COPY --from=build /app/dist /usr/share/nginx/html

# SPA-aware nginx config as a template. ${PORT} is substituted at startup; nginx's
# own runtime variables (e.g. $uri) are written literally and preserved.
RUN cat > /etc/nginx/conf.d/default.conf.template <<'NGINXCONF'
server {
    listen       ${PORT};
    server_name  _;
    root   /usr/share/nginx/html;
    index  index.html;

    # React Router: serve index.html for any unmatched path (deep links + refresh).
    location / {
        try_files $uri $uri/ /index.html;
    }
}
NGINXCONF

# Substitute ONLY $PORT (so nginx's $uri survives), then run nginx in the foreground.
CMD ["/bin/sh", "-c", "envsubst '${PORT}' < /etc/nginx/conf.d/default.conf.template > /etc/nginx/conf.d/default.conf && exec nginx -g 'daemon off;'"]
