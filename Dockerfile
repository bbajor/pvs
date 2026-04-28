# Multi-stage build for optimized production image
FROM gradle:8.10-jdk21 AS build

WORKDIR /app

# Copy dependency files first for better caching
COPY build.gradle settings.gradle gradle.properties ./
COPY gradle/ gradle/
# Copy pvs-common module (required by settings.gradle)
COPY pvs-common/ pvs-common/
# Dependency-Download mit Cache (nur wenn nicht im CI)
RUN gradle dependencies --no-daemon --build-cache --parallel || true

# Copy source code
COPY src/ src/

# Build the application (Tests bereits im Workflow ausgeführt)
# Nutze --build-cache und --parallel für schnellere Builds
# Layered JAR für besseres Caching aktivieren
# First attempt without --offline (dependencies may not be cached), then with --offline as fallback
RUN gradle bootJar --no-daemon -x test --build-cache --parallel || \
    gradle bootJar --no-daemon -x test --build-cache --parallel --offline

# Production stage - use distroless or minimal JRE image
FROM eclipse-temurin:21-jre-jammy

# Install curl for health checks (minimal)
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/*

# Create non-root user for security
RUN groupadd -r appuser && useradd -r -g appuser appuser

WORKDIR /app

# Copy JAR from build stage
COPY --from=build --chown=appuser:appuser /app/build/libs/*.jar app.jar

# Switch to non-root user
USER appuser

# Expose port (default Spring Boot port, can be overridden via PORT env var)
EXPOSE 8080

# Health check (using curl for faster checks)
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# JVM optimization flags for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
