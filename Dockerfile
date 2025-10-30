# Multi-stage build for optimized production image
FROM gradle:8.10-jdk21 AS build

WORKDIR /app

# Copy dependency files first for better caching
COPY build.gradle settings.gradle gradle.properties ./
COPY gradle/ gradle/
# Dependency-Download mit Cache
RUN gradle dependencies --no-daemon --build-cache --parallel || true

# Copy source code
COPY src/ src/

# Build production frontend bundle so runtime doesn't need dev bundling
# Use npm explicitly to avoid extra pnpm/bootstrap downloads inside container
RUN gradle clean vaadinBuildFrontend --no-daemon \
      -Pvaadin.productionMode \
      -Pvaadin.frontend.packageManager=npm \
      -Pvaadin.frontend.forceInstall=true \
      --build-cache --parallel || \
    gradle clean vaadinBuildFrontend --no-daemon \
      -Pvaadin.productionMode \
      -Pvaadin.frontend.packageManager=npm \
      -Pvaadin.frontend.forceInstall=true \
      --build-cache --parallel

# Build the application (Tests bereits im Workflow ausgeführt)
# Nutze --build-cache und --parallel für schnellere Builds
RUN gradle bootJar --no-daemon -x test --build-cache --parallel -Pvaadin.productionMode --offline || \
    gradle bootJar --no-daemon -x test --build-cache --parallel -Pvaadin.productionMode

# Production stage
FROM eclipse-temurin:21-jre-jammy

# Create non-root user for security
RUN groupadd -r appuser && useradd -r -g appuser appuser

WORKDIR /app

# Copy JAR from build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Set ownership
RUN chown -R appuser:appuser /app

# Switch to non-root user
USER appuser

# Expose port (default Spring Boot port, can be overridden via PORT env var)
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD java -jar app.jar --spring.boot.admin.client.instance.management-url=http://localhost:8080/actuator || exit 1

# JVM optimization flags for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
