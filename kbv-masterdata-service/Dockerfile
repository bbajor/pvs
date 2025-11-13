FROM eclipse-temurin:21-jdk AS builder
WORKDIR /workspace
COPY . .
RUN ./mvnw -q -DskipTests package || (curl -sL https://repo1.maven.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.tar.gz | tar xz -C /tmp && /tmp/apache-maven-3.9.9/bin/mvn -q -DskipTests package)

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /workspace/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
