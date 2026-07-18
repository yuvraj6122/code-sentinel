
# ---- Stage 1: build the frontend (static assets) --------------------------
FROM node:20-slim AS frontend
WORKDIR /app/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# ---- Stage 2: build the backend (Spring Boot fat jar) ---------------------
FROM eclipse-temurin:21-jdk AS backend
WORKDIR /workspace/backend
COPY backend/ ./
RUN chmod +x gradlew \
    && ./gradlew --no-daemon clean bootJar \
    && cp "$(find build/libs -name '*.jar' ! -name '*-plain.jar' | head -n1)" /app.jar

# ---- Stage 3: runtime image (nginx + JDK + build tools) -------------------
FROM eclipse-temurin:21-jdk AS runtime

# JDK is required (not just a JRE): the security agent compiles cloned repos.
# maven/gradle/git are best-effort build tools for repos without a wrapper.
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        nginx supervisor maven gradle git curl ca-certificates \
    && rm -rf /var/lib/apt/lists/* \
    && rm -f /etc/nginx/sites-enabled/default

# Extract the Spring Boot fat jar into an exploded layout. SpotBugs/FindSecBugs
# resolve their own jars as real files on disk to load plugin descriptors; that
# fails when they're nested inside a fat jar ("URI scheme is not file"), so we
# run from the unpacked classpath instead.
COPY --from=backend /app.jar /tmp/app.jar
RUN mkdir -p /app/unpacked \
    && (cd /app/unpacked && jar xf /tmp/app.jar) \
    && rm /tmp/app.jar
COPY --from=frontend /app/frontend/dist /usr/share/nginx/html

# nginx: serve the SPA and proxy the API. Analysis requests clone and build
# repositories, so the proxy timeouts are generous.
RUN cat > /etc/nginx/conf.d/default.conf <<'EOF'
server {
    listen 80 default_server;
    server_name _;

    root /usr/share/nginx/html;
    index index.html;

    client_max_body_size 20m;

    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_connect_timeout 60s;
        proxy_read_timeout 900s;
        proxy_send_timeout 900s;
    }

    location / {
        try_files $uri $uri/ /index.html;
    }
}
EOF

# supervisord: run the backend and nginx side by side, logging to stdout.
RUN cat > /etc/supervisor/conf.d/codesentinel.conf <<'EOF'
[program:backend]
command=java %(ENV_JAVA_OPTS)s -cp /app/unpacked/BOOT-INF/classes:/app/unpacked/BOOT-INF/lib/* com.codesentinel.BackendApplication
autostart=true
autorestart=true
stopasgroup=true
killasgroup=true
priority=10
stdout_logfile=/dev/stdout
stdout_logfile_maxbytes=0
stderr_logfile=/dev/stderr
stderr_logfile_maxbytes=0

[program:nginx]
command=nginx -g 'daemon off;'
autostart=true
autorestart=true
priority=20
stdout_logfile=/dev/stdout
stdout_logfile_maxbytes=0
stderr_logfile=/dev/stderr
stderr_logfile_maxbytes=0
EOF

# Overridable at runtime. Datasource intentionally left unset here so you can
# point it at your RDS/postgres with -e SPRING_DATASOURCE_URL=...
ENV JAVA_OPTS=""

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=5 \
    CMD curl -fsS http://localhost/api/health || exit 1

CMD ["/usr/bin/supervisord", "-n", "-c", "/etc/supervisor/supervisord.conf"]
