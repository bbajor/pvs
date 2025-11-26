# basics

Write your command content here.

This command will be available in chat with /basics

Build (gradle, cross-OS):
./gradlew clean build -x test
.\gradlew.bat clean build -x test
Full test suite (mit Testcontainers):
./gradlew test
Run dev (Spring profile=dev, Vaadin dev server):
./gradlew bootRun --args='--spring.profiles.active=dev'
Run prod locally (optimized, no dev server):
./gradlew -Pvaadin.productionMode=true vaadinBuildFrontend bootRun --args='--spring.profiles.active=prod'
Vaadin production bundle:
./gradlew -Pvaadin.productionMode=true vaadinBuildFrontend
Lint/Static Analysis:
./gradlew checkstyleMain pmdMain spotbugsMain
Format (falls konfiguriert) oder Verify:
./gradlew spotlessApply || ./gradlew spotlessCheck
Dependency/CVE Scan (OWASP Dependency-Check, falls eingebunden):
./gradlew dependencyCheckAnalyze
OpenAPI/Docs (wenn vorhanden):
./gradlew openApiGenerate || ./gradlew asciidoctor
Flyway validate/migrate (vorsichtig in dev/test):
./gradlew flywayValidate
./gradlew flywayMigrate -Dflyway.configFiles=src/main/resources/flyway.conf
Docker image (Spring Boot Buildpacks):
./gradlew bootBuildImage --imageName pvs-app:local
Compose dev env:
docker compose -f docker-compose.dev.yml --env-file docker-compose.dev.env up -d
Smoke perf (simple startup + queries cap, falls Tasks vorhanden):
./gradlew :startUpSmokeTest