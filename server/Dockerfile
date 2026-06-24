# syntax=docker/dockerfile:1

# ----------------------------------------------------------------------------
# Stage 1 — build the executable jar
# ----------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Resolve dependencies first so this layer is cached unless pom.xml changes.
COPY pom.xml ./
RUN mvn -B -ntp dependency:go-offline

# Compile + package. Tests run in CI, so skip them here for a fast, reproducible image.
COPY src ./src
RUN mvn -B -ntp clean package -DskipTests

# ----------------------------------------------------------------------------
# Stage 2 — slim runtime image (JRE only, no Maven/JDK)
# ----------------------------------------------------------------------------
FROM eclipse-temurin:17-jre AS runtime
WORKDIR /app

# finalName in pom.xml is "server", so the artifact is target/server.jar.
COPY --from=build /app/target/server.jar app.jar

# Render injects $PORT. Spring already binds 0.0.0.0 by default; we just point it
# at $PORT. Cap the heap so the JVM stays within the instance memory (e.g. 512 MB).
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"
ENV PORT=8080
EXPOSE 8080

CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar --server.port=${PORT}"]
