FROM eclipse-temurin:21-jre
# Copy Spring Boot fat jar built by Gradle
COPY build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
