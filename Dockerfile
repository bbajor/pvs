FROM eclipse-temurin:21-jdk AS builder
WORKDIR /workspace
COPY . .
RUN ./gradlew --no-daemon -q clean bootJar

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /workspace/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
