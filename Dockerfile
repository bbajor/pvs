# Multi-stage build for optimized production image
FROM gradle:8.10-jdk21 AS build

# Install Node.js and npm for Vaadin frontend build
# Use NodeSource repository for latest LTS version
RUN curl -fsSL https://deb.nodesource.com/setup_20.x | bash - && \
    apt-get install -y nodejs && \
    npm install -g npm@latest && \
    node --version && \
    npm --version

WORKDIR /app

# Copy dependency files first for better caching
COPY build.gradle settings.gradle gradle.properties ./
COPY gradle/ gradle/
# Dependency-Download mit Cache (nur wenn nicht im CI)
RUN gradle dependencies --no-daemon --build-cache --parallel || true

# Copy source code
COPY src/ src/

# Build production frontend bundle so runtime doesn't need dev bundling
# Use npm explicitly to avoid extra pnpm/bootstrap downloads inside container
# classes task (includes compileJava + processResources) must run before vaadinBuildFrontend
# to ensure all compiled classes and dependencies are available for annotation scanning
RUN echo "🔨 Building classes..." && \
    gradle clean classes --no-daemon --build-cache --parallel && \
    echo "✅ Classes built successfully" && \
    echo "🔨 Building Vaadin frontend..." && \
    gradle vaadinBuildFrontend --no-daemon \
      -Pvaadin.productionMode \
      -Pvaadin.frontend.packageManager=npm \
      -Pvaadin.frontend.forceInstall=true \
      --build-cache --stacktrace || \
    (echo "⚠️ First attempt failed, retrying without parallel..." && \
     gradle clean classes --no-daemon --build-cache && \
     gradle vaadinBuildFrontend --no-daemon \
       -Pvaadin.productionMode \
       -Pvaadin.frontend.packageManager=npm \
       -Pvaadin.frontend.forceInstall=true \
       --build-cache --stacktrace)

# Build the application (Tests bereits im Workflow ausgeführt)
# Nutze --build-cache und --parallel für schnellere Builds
# Layered JAR für besseres Caching aktivieren
RUN gradle bootJar --no-daemon -x test --build-cache --parallel -Pvaadin.productionMode --offline || \
    gradle bootJar --no-daemon -x test --build-cache --parallel -Pvaadin.productionMode

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
