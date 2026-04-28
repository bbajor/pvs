# Third-Party Software Licenses

This document lists all third-party software libraries and their licenses used in this project.

**Last Updated:** 2026-04-28 10:19:14

## License Summary

All dependencies use permissive open-source licenses that allow commercial use:

- **Apache License 2.0**: Most dependencies
- **MIT License**: Lombok
- **BSD-2-Clause / PostgreSQL License**: PostgreSQL JDBC Driver
- **MPL 2.0 / EPL 1.0**: H2 Database
- **Bouncy Castle License**: Bouncy Castle libraries

## Direct Dependencies

### Application Dependencies

| Library | Version | License | Commercial Use |
|---------|---------|---------|----------------|
| ch.qos.logback:logback-classic | 1.5.20 | Apache License 2.0 | ✅ Yes |
| ch.qos.logback:logback-core | 1.5.20 | Apache License 2.0 | ✅ Yes |
| com.beust:jcommander | 1.82 | Apache License 2.0 | ✅ Yes |
| com.fasterxml.jackson.core:jackson-annotations | 2.19.2 | Apache License 2.0 | ✅ Yes |
| com.fasterxml.jackson.core:jackson-core | 2.19.2 | Apache License 2.0 | ✅ Yes |
| com.fasterxml.jackson.core:jackson-databind | 2.19.2 | Apache License 2.0 | ✅ Yes |
| com.fasterxml.jackson.dataformat:jackson-dataformat-toml | 2.19.2 | Apache License 2.0 | ✅ Yes |
| com.fasterxml.jackson.dataformat:jackson-dataformat-yaml | 2.19.2 | Apache License 2.0 | ✅ Yes |
| com.fasterxml.jackson.datatype:jackson-datatype-jdk8 | 2.19.2 | Apache License 2.0 | ✅ Yes |
| com.fasterxml.jackson.datatype:jackson-datatype-jsr310 | 2.19.2 | Apache License 2.0 | ✅ Yes |
| com.fasterxml.jackson.module:jackson-module-parameter-names | 2.19.2 | Apache License 2.0 | ✅ Yes |
| com.fasterxml:classmate | 1.7.1 | Apache License 2.0 | ✅ Yes |
| com.github.jai-imageio:jai-imageio-core | 1.4.0 | Apache License 2.0 | ✅ Yes |
| com.github.stephenc.jcip:jcip-annotations | 1.0-1 | Apache License 2.0 | ✅ Yes |
| com.google.code.findbugs:jsr305 | 3.0.2 | Apache License 2.0 | ✅ Yes |
| com.google.zxing:core | 3.5.3 | Apache License 2.0 | ✅ Yes |
| com.google.zxing:javase | 3.5.3 | Apache License 2.0 | ✅ Yes |
| com.h2database:h2 | 2.3.232 | MPL 2.0 / EPL 1.0 | ✅ Yes |
| com.nimbusds:nimbus-jose-jwt | 9.37.4 | Apache License 2.0 | ✅ Yes |
| com.sun.istack:istack-commons-runtime | 4.1.2 | Apache License 2.0 | ✅ Yes |
| com.warrenstrange:googleauth | 1.5.0 | Apache License 2.0 | ✅ Yes |
| com.zaxxer:HikariCP | 6.3.3 | Apache License 2.0 | ✅ Yes |
| commons-codec:commons-codec | 1.18.0 | Apache License 2.0 | ✅ Yes |
| commons-io:commons-io | 2.20.0 | Apache License 2.0 | ✅ Yes |
| de.bbajor.pvs:pvs-common | 1.0-SNAPSHOT | Apache License 2.0 | ✅ Yes |
| io.github.resilience4j:resilience4j-annotations | 2.2.0 | Apache License 2.0 | ✅ Yes |
| io.github.resilience4j:resilience4j-bulkhead | 2.2.0 | Apache License 2.0 | ✅ Yes |
| io.github.resilience4j:resilience4j-circuitbreaker | 2.2.0 | Apache License 2.0 | ✅ Yes |
| io.github.resilience4j:resilience4j-circularbuffer | 2.2.0 | Apache License 2.0 | ✅ Yes |
| io.github.resilience4j:resilience4j-consumer | 2.2.0 | Apache License 2.0 | ✅ Yes |
| io.github.resilience4j:resilience4j-core | 2.2.0 | Apache License 2.0 | ✅ Yes |
| io.github.resilience4j:resilience4j-framework-common | 2.2.0 | Apache License 2.0 | ✅ Yes |
| io.github.resilience4j:resilience4j-micrometer | 2.2.0 | Apache License 2.0 | ✅ Yes |
| io.github.resilience4j:resilience4j-ratelimiter | 2.2.0 | Apache License 2.0 | ✅ Yes |
| io.github.resilience4j:resilience4j-retry | 2.2.0 | Apache License 2.0 | ✅ Yes |
| io.github.resilience4j:resilience4j-spring-boot3 | 2.2.0 | Apache License 2.0 | ✅ Yes |
| io.github.resilience4j:resilience4j-spring6 | 2.2.0 | Apache License 2.0 | ✅ Yes |
| io.github.resilience4j:resilience4j-timelimiter | 2.2.0 | Apache License 2.0 | ✅ Yes |
| io.micrometer:micrometer-commons | 1.15.5 | Apache License 2.0 | ✅ Yes |
| io.micrometer:micrometer-core | 1.15.5 | Apache License 2.0 | ✅ Yes |
| io.micrometer:micrometer-jakarta9 | 1.15.5 | Apache License 2.0 | ✅ Yes |
| io.micrometer:micrometer-observation | 1.15.5 | Apache License 2.0 | ✅ Yes |
| io.micrometer:micrometer-registry-prometheus | 1.15.5 | Apache License 2.0 | ✅ Yes |
| io.netty:netty-buffer | 4.1.128.Final | Apache License 2.0 | ✅ Yes |
| io.netty:netty-codec | 4.1.128.Final | Apache License 2.0 | ✅ Yes |
| io.netty:netty-codec-dns | 4.1.128.Final | Apache License 2.0 | ✅ Yes |
| io.netty:netty-codec-http | 4.1.128.Final | Apache License 2.0 | ✅ Yes |
| io.netty:netty-codec-http2 | 4.1.128.Final | Apache License 2.0 | ✅ Yes |
| io.netty:netty-codec-socks | 4.1.128.Final | Apache License 2.0 | ✅ Yes |
| io.netty:netty-common | 4.1.128.Final | Apache License 2.0 | ✅ Yes |
| io.netty:netty-handler | 4.1.128.Final | Apache License 2.0 | ✅ Yes |
| io.netty:netty-handler-proxy | 4.1.128.Final | Apache License 2.0 | ✅ Yes |
| io.netty:netty-resolver | 4.1.128.Final | Apache License 2.0 | ✅ Yes |
| io.netty:netty-resolver-dns | 4.1.128.Final | Apache License 2.0 | ✅ Yes |
| io.netty:netty-resolver-dns-classes-macos | 4.1.128.Final | Apache License 2.0 | ✅ Yes |
| io.netty:netty-resolver-dns-native-macos | 4.1.128.Final | Apache License 2.0 | ✅ Yes |
| io.netty:netty-transport | 4.1.128.Final | Apache License 2.0 | ✅ Yes |
| io.netty:netty-transport-classes-epoll | 4.1.128.Final | Apache License 2.0 | ✅ Yes |
| io.netty:netty-transport-native-epoll | 4.1.128.Final | Apache License 2.0 | ✅ Yes |
| io.netty:netty-transport-native-unix-common | 4.1.128.Final | Apache License 2.0 | ✅ Yes |
| io.projectreactor.addons:reactor-extra | 3.5.4 | Apache License 2.0 | ✅ Yes |
| io.projectreactor.netty:reactor-netty-core | 1.2.11 | Apache License 2.0 | ✅ Yes |
| io.projectreactor.netty:reactor-netty-http | 1.2.11 | Apache License 2.0 | ✅ Yes |
| io.projectreactor:reactor-core | 3.7.12 | Apache License 2.0 | ✅ Yes |
| io.prometheus:prometheus-metrics-config | 1.3.10 | Apache License 2.0 | ✅ Yes |
| io.prometheus:prometheus-metrics-core | 1.3.10 | Apache License 2.0 | ✅ Yes |
| io.prometheus:prometheus-metrics-exposition-formats | 1.3.10 | Apache License 2.0 | ✅ Yes |
| io.prometheus:prometheus-metrics-exposition-textformats | 1.3.10 | Apache License 2.0 | ✅ Yes |
| io.prometheus:prometheus-metrics-model | 1.3.10 | Apache License 2.0 | ✅ Yes |
| io.prometheus:prometheus-metrics-tracer-common | 1.3.10 | Apache License 2.0 | ✅ Yes |
| io.smallrye:jandex | 3.2.0 | Apache License 2.0 | ✅ Yes |
| io.swagger.core.v3:swagger-annotations-jakarta | 2.2.22 | Apache License 2.0 | ✅ Yes |
| io.swagger.core.v3:swagger-core-jakarta | 2.2.22 | Apache License 2.0 | ✅ Yes |
| io.swagger.core.v3:swagger-models-jakarta | 2.2.22 | Apache License 2.0 | ✅ Yes |
| jakarta.activation:jakarta.activation-api | 2.1.4 | Apache License 2.0 | ✅ Yes |
| jakarta.annotation:jakarta.annotation-api | 2.1.1 | Apache License 2.0 | ✅ Yes |
| jakarta.inject:jakarta.inject-api | 2.0.1 | Apache License 2.0 | ✅ Yes |
| jakarta.persistence:jakarta.persistence-api | 3.1.0 | Apache License 2.0 | ✅ Yes |
| jakarta.transaction:jakarta.transaction-api | 2.0.1 | Apache License 2.0 | ✅ Yes |
| jakarta.validation:jakarta.validation-api | 3.0.2 | Apache License 2.0 | ✅ Yes |
| jakarta.xml.bind:jakarta.xml.bind-api | 4.0.4 | Apache License 2.0 | ✅ Yes |
| net.bytebuddy:byte-buddy | 1.17.8 | Apache License 2.0 | ✅ Yes |
| net.logstash.logback:logstash-logback-encoder | 7.4 | Apache License 2.0 | ✅ Yes |
| org.antlr:antlr4-runtime | 4.13.0 | Apache License 2.0 | ✅ Yes |
| org.apache.ant:ant | 1.10.6 | Apache License 2.0 | ✅ Yes |
| org.apache.ant:ant-launcher | 1.10.6 | Apache License 2.0 | ✅ Yes |
| org.apache.commons:commons-csv | 1.14.1 | Apache License 2.0 | ✅ Yes |
| org.apache.commons:commons-lang3 | 3.17.0 | Apache License 2.0 | ✅ Yes |
| org.apache.httpcomponents:httpclient | 4.5.12 | Apache License 2.0 | ✅ Yes |
| org.apache.httpcomponents:httpcore | 4.4.16 | Apache License 2.0 | ✅ Yes |
| org.apache.logging.log4j:log4j-api | 2.24.3 | Apache License 2.0 | ✅ Yes |
| org.apache.logging.log4j:log4j-to-slf4j | 2.24.3 | Apache License 2.0 | ✅ Yes |
| org.apache.pdfbox:fontbox | 3.0.1 | Apache License 2.0 | ✅ Yes |
| org.apache.pdfbox:pdfbox | 3.0.1 | Apache License 2.0 | ✅ Yes |
| org.apache.pdfbox:pdfbox-io | 3.0.1 | Apache License 2.0 | ✅ Yes |
| org.apache.tomcat.embed:tomcat-embed-core | 10.1.48 | Apache License 2.0 | ✅ Yes |
| org.apache.tomcat.embed:tomcat-embed-el | 10.1.48 | Apache License 2.0 | ✅ Yes |
| org.apache.tomcat.embed:tomcat-embed-websocket | 10.1.48 | Apache License 2.0 | ✅ Yes |
| org.apiguardian:apiguardian-api | 1.1.2 | Apache License 2.0 | ✅ Yes |
| org.aspectj:aspectjweaver | 1.9.24 | Apache License 2.0 | ✅ Yes |
| org.bouncycastle:bcpg-jdk18on | 1.78.1 | Bouncy Castle License | ✅ Yes |
| org.bouncycastle:bcprov-jdk18on | 1.80 | Bouncy Castle License | ✅ Yes |
| org.bouncycastle:bcutil-jdk18on | 1.80 | Bouncy Castle License | ✅ Yes |
| org.checkerframework:checker-qual | 3.49.5 | Apache License 2.0 | ✅ Yes |
| org.eclipse.angus:angus-activation | 2.0.3 | Apache License 2.0 | ✅ Yes |
| org.eclipse.angus:jakarta.mail | 2.0.5 | Apache License 2.0 | ✅ Yes |
| org.flywaydb:flyway-core | 11.7.2 | Apache License 2.0 | ✅ Yes |
| org.flywaydb:flyway-database-postgresql | 11.7.2 | Apache License 2.0 | ✅ Yes |
| org.glassfish.jaxb:jaxb-core | 4.0.6 | Apache License 2.0 | ✅ Yes |
| org.glassfish.jaxb:jaxb-runtime | 4.0.6 | Apache License 2.0 | ✅ Yes |
| org.glassfish.jaxb:txw2 | 4.0.6 | Apache License 2.0 | ✅ Yes |
| org.hdrhistogram:HdrHistogram | 2.2.2 | Apache License 2.0 | ✅ Yes |
| org.hibernate.common:hibernate-commons-annotations | 7.0.3.Final | Apache License 2.0 | ✅ Yes |
| org.hibernate.orm:hibernate-core | 6.6.33.Final | Apache License 2.0 | ✅ Yes |
| org.hibernate.validator:hibernate-validator | 8.0.3.Final | Apache License 2.0 | ✅ Yes |
| org.instancio:instancio-core | 5.5.1 | Apache License 2.0 | ✅ Yes |
| org.jboss.logging:jboss-logging | 3.6.1.Final | Apache License 2.0 | ✅ Yes |
| org.jspecify:jspecify | 1.0.0 | Apache License 2.0 | ✅ Yes |
| org.junit.jupiter:junit-jupiter | 5.12.2 | Apache License 2.0 | ✅ Yes |
| org.junit.jupiter:junit-jupiter-api | 5.12.2 | Apache License 2.0 | ✅ Yes |
| org.junit.jupiter:junit-jupiter-engine | 5.12.2 | Apache License 2.0 | ✅ Yes |
| org.junit.jupiter:junit-jupiter-params | 5.12.2 | Apache License 2.0 | ✅ Yes |
| org.junit.platform:junit-platform-commons | 1.12.2 | Apache License 2.0 | ✅ Yes |
| org.junit.platform:junit-platform-engine | 1.12.2 | Apache License 2.0 | ✅ Yes |
| org.latencyutils:LatencyUtils | 2.0.3 | Apache License 2.0 | ✅ Yes |
| org.mapstruct:mapstruct | 1.5.5.Final | Apache License 2.0 | ✅ Yes |
| org.mapstruct:mapstruct-processor | 1.5.5.Final | Apache License 2.0 | ✅ Yes |
| org.opentest4j:opentest4j | 1.3.0 | Apache License 2.0 | ✅ Yes |
| org.pgpainless:pgpainless-core | 1.6.9 | Apache License 2.0 | ✅ Yes |
| org.postgresql:postgresql | 42.7.8 | BSD-2-Clause / PostgreSQL License | ✅ Yes |
| org.projectlombok:lombok | 1.18.42 | MIT License | ✅ Yes |
| org.projectlombok:lombok-mapstruct-binding | 0.2.0 | Apache License 2.0 | ✅ Yes |
| org.reactivestreams:reactive-streams | 1.0.4 | Apache License 2.0 | ✅ Yes |
| org.slf4j:jul-to-slf4j | 2.0.17 | Apache License 2.0 | ✅ Yes |
| org.slf4j:slf4j-api | 2.0.17 | Apache License 2.0 | ✅ Yes |
| org.springdoc:springdoc-openapi-starter-common | 2.6.0 | Apache License 2.0 | ✅ Yes |
| org.springdoc:springdoc-openapi-starter-webmvc-api | 2.6.0 | Apache License 2.0 | ✅ Yes |
| org.springdoc:springdoc-openapi-starter-webmvc-ui | 2.6.0 | Apache License 2.0 | ✅ Yes |
| org.springframework.boot:spring-boot | 3.5.7 | Apache License 2.0 | ✅ Yes |
| org.springframework.boot:spring-boot-actuator | 3.5.7 | Apache License 2.0 | ✅ Yes |
| org.springframework.boot:spring-boot-actuator-autoconfigure | 3.5.7 | Apache License 2.0 | ✅ Yes |
| org.springframework.boot:spring-boot-autoconfigure | 3.5.7 | Apache License 2.0 | ✅ Yes |
| org.springframework.boot:spring-boot-devtools | 3.5.7 | Apache License 2.0 | ✅ Yes |
| org.springframework.boot:spring-boot-starter | 3.5.7 | Apache License 2.0 | ✅ Yes |
| org.springframework.boot:spring-boot-starter-actuator | 3.5.7 | Apache License 2.0 | ✅ Yes |
| org.springframework.boot:spring-boot-starter-data-jpa | 3.5.7 | Apache License 2.0 | ✅ Yes |
| org.springframework.boot:spring-boot-starter-jdbc | 3.5.7 | Apache License 2.0 | ✅ Yes |
| org.springframework.boot:spring-boot-starter-json | 3.5.7 | Apache License 2.0 | ✅ Yes |
| org.springframework.boot:spring-boot-starter-logging | 3.5.7 | Apache License 2.0 | ✅ Yes |
| org.springframework.boot:spring-boot-starter-mail | 3.5.7 | Apache License 2.0 | ✅ Yes |
| org.springframework.boot:spring-boot-starter-oauth2-resource-server | 3.5.7 | Apache License 2.0 | ✅ Yes |
| org.springframework.boot:spring-boot-starter-reactor-netty | 3.5.7 | Apache License 2.0 | ✅ Yes |
| org.springframework.boot:spring-boot-starter-security | 3.5.7 | Apache License 2.0 | ✅ Yes |
| org.springframework.boot:spring-boot-starter-tomcat | 3.5.7 | Apache License 2.0 | ✅ Yes |
| org.springframework.boot:spring-boot-starter-validation | 3.5.7 | Apache License 2.0 | ✅ Yes |
| org.springframework.boot:spring-boot-starter-web | 3.5.7 | Apache License 2.0 | ✅ Yes |
| org.springframework.boot:spring-boot-starter-webflux | 3.5.7 | Apache License 2.0 | ✅ Yes |
| org.springframework.cloud:spring-cloud-commons | 4.2.0 | Apache License 2.0 | ✅ Yes |
| org.springframework.cloud:spring-cloud-context | 4.2.0 | Apache License 2.0 | ✅ Yes |
| org.springframework.cloud:spring-cloud-function-adapter-aws | 4.2.0 | Apache License 2.0 | ✅ Yes |
| org.springframework.cloud:spring-cloud-function-context | 4.2.0 | Apache License 2.0 | ✅ Yes |
| org.springframework.cloud:spring-cloud-function-core | 4.2.0 | Apache License 2.0 | ✅ Yes |
| org.springframework.cloud:spring-cloud-function-web | 4.2.0 | Apache License 2.0 | ✅ Yes |
| org.springframework.cloud:spring-cloud-gateway-server | 4.2.0 | Apache License 2.0 | ✅ Yes |
| org.springframework.cloud:spring-cloud-starter | 4.2.0 | Apache License 2.0 | ✅ Yes |
| org.springframework.cloud:spring-cloud-starter-gateway | 4.2.0 | Apache License 2.0 | ✅ Yes |
| org.springframework.data:spring-data-commons | 3.5.5 | Apache License 2.0 | ✅ Yes |
| org.springframework.data:spring-data-jpa | 3.5.5 | Apache License 2.0 | ✅ Yes |
| org.springframework.security:spring-security-config | 6.5.6 | Apache License 2.0 | ✅ Yes |
| org.springframework.security:spring-security-core | 6.5.6 | Apache License 2.0 | ✅ Yes |
| org.springframework.security:spring-security-crypto | 6.5.6 | Apache License 2.0 | ✅ Yes |
| org.springframework.security:spring-security-oauth2-core | 6.5.6 | Apache License 2.0 | ✅ Yes |
| org.springframework.security:spring-security-oauth2-jose | 6.5.6 | Apache License 2.0 | ✅ Yes |
| org.springframework.security:spring-security-oauth2-resource-server | 6.5.6 | Apache License 2.0 | ✅ Yes |
| org.springframework.security:spring-security-web | 6.5.6 | Apache License 2.0 | ✅ Yes |
| org.springframework:spring-aop | 6.2.12 | Apache License 2.0 | ✅ Yes |
| org.springframework:spring-aspects | 6.2.12 | Apache License 2.0 | ✅ Yes |
| org.springframework:spring-beans | 6.2.12 | Apache License 2.0 | ✅ Yes |
| org.springframework:spring-context | 6.2.12 | Apache License 2.0 | ✅ Yes |
| org.springframework:spring-context-support | 6.2.12 | Apache License 2.0 | ✅ Yes |
| org.springframework:spring-core | 6.2.12 | Apache License 2.0 | ✅ Yes |
| org.springframework:spring-expression | 6.2.12 | Apache License 2.0 | ✅ Yes |
| org.springframework:spring-jcl | 6.2.12 | Apache License 2.0 | ✅ Yes |
| org.springframework:spring-jdbc | 6.2.12 | Apache License 2.0 | ✅ Yes |
| org.springframework:spring-messaging | 6.2.12 | Apache License 2.0 | ✅ Yes |
| org.springframework:spring-orm | 6.2.12 | Apache License 2.0 | ✅ Yes |
| org.springframework:spring-tx | 6.2.12 | Apache License 2.0 | ✅ Yes |
| org.springframework:spring-web | 6.2.12 | Apache License 2.0 | ✅ Yes |
| org.springframework:spring-webflux | 6.2.12 | Apache License 2.0 | ✅ Yes |
| org.springframework:spring-webmvc | 6.2.12 | Apache License 2.0 | ✅ Yes |
| org.webjars:swagger-ui | 5.17.14 | Apache License 2.0 | ✅ Yes |
| org.yaml:snakeyaml | 2.4 | Apache License 2.0 | ✅ Yes |

### Test Dependencies

| Library | Version | License | Commercial Use |
|---------|---------|---------|----------------|
| com.github.docker-java:docker-java | 3.3.4 | Apache License 2.0 | ✅ Yes |
| com.github.docker-java:docker-java-api | 3.4.2 | Apache License 2.0 | ✅ Yes |
| com.github.docker-java:docker-java-core | 3.3.4 | Apache License 2.0 | ✅ Yes |
| com.github.docker-java:docker-java-transport | 3.4.2 | Apache License 2.0 | ✅ Yes |
| com.github.docker-java:docker-java-transport-httpclient5 | 3.3.4 | Apache License 2.0 | ✅ Yes |
| com.github.docker-java:docker-java-transport-zerodep | 3.4.2 | Apache License 2.0 | ✅ Yes |
| com.google.auto.service:auto-service-annotations | 1.1.1 | Apache License 2.0 | ✅ Yes |
| com.google.code.gson:gson | 2.13.2 | Apache License 2.0 | ✅ Yes |
| com.google.errorprone:error_prone_annotations | 2.41.0 | Apache License 2.0 | ✅ Yes |
| com.google.guava:failureaccess | 1.0.3 | Apache License 2.0 | ✅ Yes |
| com.google.guava:guava | 33.4.6-jre | Apache License 2.0 | ✅ Yes |
| com.google.guava:listenablefuture | 9999.0-empty-to-avoid-conflict-with-guava | Apache License 2.0 | ✅ Yes |
| com.google.j2objc:j2objc-annotations | 3.0.0 | Apache License 2.0 | ✅ Yes |
| com.jayway.jsonpath:json-path | 2.9.0 | Apache License 2.0 | ✅ Yes |
| com.tngtech.archunit:archunit | 1.4.1 | Apache License 2.0 | ✅ Yes |
| com.tngtech.archunit:archunit-junit5 | 1.4.1 | Apache License 2.0 | ✅ Yes |
| com.tngtech.archunit:archunit-junit5-api | 1.4.1 | Apache License 2.0 | ✅ Yes |
| com.tngtech.archunit:archunit-junit5-engine | 1.4.1 | Apache License 2.0 | ✅ Yes |
| com.tngtech.archunit:archunit-junit5-engine-api | 1.4.1 | Apache License 2.0 | ✅ Yes |
| com.vaadin.external.google:android-json | 0.0.20131108.vaadin1 | Apache License 2.0 | ✅ Yes |
| io.github.bonigarcia:selenium-jupiter | 5.0.1 | Apache License 2.0 | ✅ Yes |
| io.github.bonigarcia:webdrivermanager | 5.6.3 | Apache License 2.0 | ✅ Yes |
| io.opentelemetry:opentelemetry-api | 1.49.0 | Apache License 2.0 | ✅ Yes |
| io.opentelemetry:opentelemetry-context | 1.49.0 | Apache License 2.0 | ✅ Yes |
| io.opentelemetry:opentelemetry-exporter-logging | 1.49.0 | Apache License 2.0 | ✅ Yes |
| io.opentelemetry:opentelemetry-sdk | 1.49.0 | Apache License 2.0 | ✅ Yes |
| io.opentelemetry:opentelemetry-sdk-common | 1.49.0 | Apache License 2.0 | ✅ Yes |
| io.opentelemetry:opentelemetry-sdk-extension-autoconfigure | 1.49.0 | Apache License 2.0 | ✅ Yes |
| io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi | 1.49.0 | Apache License 2.0 | ✅ Yes |
| io.opentelemetry:opentelemetry-sdk-logs | 1.49.0 | Apache License 2.0 | ✅ Yes |
| io.opentelemetry:opentelemetry-sdk-metrics | 1.49.0 | Apache License 2.0 | ✅ Yes |
| io.opentelemetry:opentelemetry-sdk-trace | 1.49.0 | Apache License 2.0 | ✅ Yes |
| junit:junit | 4.13.2 | Apache License 2.0 | ✅ Yes |
| net.bytebuddy:byte-buddy-agent | 1.17.8 | Apache License 2.0 | ✅ Yes |
| net.java.dev.jna:jna | 5.13.0 | Apache License 2.0 | ✅ Yes |
| net.minidev:accessors-smart | 2.5.2 | Apache License 2.0 | ✅ Yes |
| net.minidev:json-smart | 2.5.2 | Apache License 2.0 | ✅ Yes |
| org.apache.commons:commons-compress | 1.25.0 | Apache License 2.0 | ✅ Yes |
| org.apache.commons:commons-exec | 1.4.0 | Apache License 2.0 | ✅ Yes |
| org.apache.httpcomponents.client5:httpclient5 | 5.5.1 | Apache License 2.0 | ✅ Yes |
| org.apache.httpcomponents.core5:httpcore5 | 5.3.6 | Apache License 2.0 | ✅ Yes |
| org.apache.httpcomponents.core5:httpcore5-h2 | 5.3.6 | Apache License 2.0 | ✅ Yes |
| org.assertj:assertj-core | 3.27.6 | Apache License 2.0 | ✅ Yes |
| org.awaitility:awaitility | 4.2.2 | Apache License 2.0 | ✅ Yes |
| org.bouncycastle:bcpkix-jdk18on | 1.76 | Bouncy Castle License | ✅ Yes |
| org.brotli:dec | 0.1.2 | Apache License 2.0 | ✅ Yes |
| org.hamcrest:hamcrest | 3.0 | Apache License 2.0 | ✅ Yes |
| org.hamcrest:hamcrest-core | 3.0 | Apache License 2.0 | ✅ Yes |
| org.instancio:instancio-junit | 5.5.1 | Apache License 2.0 | ✅ Yes |
| org.jetbrains:annotations | 17.0.0 | Apache License 2.0 | ✅ Yes |
| org.junit.platform:junit-platform-launcher | 1.12.2 | Apache License 2.0 | ✅ Yes |
| org.mockito:mockito-core | 5.17.0 | Apache License 2.0 | ✅ Yes |
| org.mockito:mockito-junit-jupiter | 5.17.0 | Apache License 2.0 | ✅ Yes |
| org.objenesis:objenesis | 3.3 | Apache License 2.0 | ✅ Yes |
| org.ow2.asm:asm | 9.7.1 | Apache License 2.0 | ✅ Yes |
| org.rnorth.duct-tape:duct-tape | 1.0.8 | Apache License 2.0 | ✅ Yes |
| org.seleniumhq.selenium:selenium-api | 4.31.0 | Apache License 2.0 | ✅ Yes |
| org.seleniumhq.selenium:selenium-chrome-driver | 4.31.0 | Apache License 2.0 | ✅ Yes |
| org.seleniumhq.selenium:selenium-chromium-driver | 4.31.0 | Apache License 2.0 | ✅ Yes |
| org.seleniumhq.selenium:selenium-devtools-v128 | 4.26.0 | Apache License 2.0 | ✅ Yes |
| org.seleniumhq.selenium:selenium-devtools-v129 | 4.26.0 | Apache License 2.0 | ✅ Yes |
| org.seleniumhq.selenium:selenium-devtools-v130 | 4.26.0 | Apache License 2.0 | ✅ Yes |
| org.seleniumhq.selenium:selenium-devtools-v85 | 4.26.0 | Apache License 2.0 | ✅ Yes |
| org.seleniumhq.selenium:selenium-edge-driver | 4.31.0 | Apache License 2.0 | ✅ Yes |
| org.seleniumhq.selenium:selenium-firefox-driver | 4.31.0 | Apache License 2.0 | ✅ Yes |
| org.seleniumhq.selenium:selenium-http | 4.31.0 | Apache License 2.0 | ✅ Yes |
| org.seleniumhq.selenium:selenium-ie-driver | 4.31.0 | Apache License 2.0 | ✅ Yes |
| org.seleniumhq.selenium:selenium-java | 4.26.0 | Apache License 2.0 | ✅ Yes |
| org.seleniumhq.selenium:selenium-json | 4.31.0 | Apache License 2.0 | ✅ Yes |
| org.seleniumhq.selenium:selenium-manager | 4.31.0 | Apache License 2.0 | ✅ Yes |
| org.seleniumhq.selenium:selenium-os | 4.31.0 | Apache License 2.0 | ✅ Yes |
| org.seleniumhq.selenium:selenium-remote-driver | 4.31.0 | Apache License 2.0 | ✅ Yes |
| org.seleniumhq.selenium:selenium-safari-driver | 4.31.0 | Apache License 2.0 | ✅ Yes |
| org.seleniumhq.selenium:selenium-support | 4.26.0 | Apache License 2.0 | ✅ Yes |
| org.skyscreamer:jsonassert | 1.5.3 | Apache License 2.0 | ✅ Yes |
| org.slf4j:jcl-over-slf4j | 2.0.17 | Apache License 2.0 | ✅ Yes |
| org.springframework.boot:spring-boot-starter-test | 3.5.7 | Apache License 2.0 | ✅ Yes |
| org.springframework.boot:spring-boot-test | 3.5.7 | Apache License 2.0 | ✅ Yes |
| org.springframework.boot:spring-boot-test-autoconfigure | 3.5.7 | Apache License 2.0 | ✅ Yes |
| org.springframework.boot:spring-boot-testcontainers | 3.5.7 | Apache License 2.0 | ✅ Yes |
| org.springframework.security:spring-security-test | 6.5.6 | Apache License 2.0 | ✅ Yes |
| org.springframework:spring-test | 6.2.12 | Apache License 2.0 | ✅ Yes |
| org.testcontainers:database-commons | 1.21.3 | Apache License 2.0 | ✅ Yes |
| org.testcontainers:jdbc | 1.21.3 | Apache License 2.0 | ✅ Yes |
| org.testcontainers:junit-jupiter | 1.21.3 | Apache License 2.0 | ✅ Yes |
| org.testcontainers:postgresql | 1.21.3 | Apache License 2.0 | ✅ Yes |
| org.testcontainers:testcontainers | 1.21.3 | Apache License 2.0 | ✅ Yes |
| org.xmlunit:xmlunit-core | 2.10.4 | Apache License 2.0 | ✅ Yes |

## License Texts

### Apache License 2.0

The majority of dependencies use the Apache License 2.0. Full text available at:
https://www.apache.org/licenses/LICENSE-2.0

### MIT License

Used by Lombok. Full text available at:
https://opensource.org/licenses/MIT

### BSD-2-Clause License

Used by PostgreSQL JDBC Driver. Full text available at:
https://opensource.org/licenses/BSD-2-Clause

### MPL 2.0 / EPL 1.0

Used by H2 Database. Full texts available at:
- MPL 2.0: https://www.mozilla.org/en-US/MPL/2.0/
- EPL 1.0: https://www.eclipse.org/legal/epl-v10.html

### Bouncy Castle License

Used by Bouncy Castle libraries. Full text available at:
https://www.bouncycastle.org/licence.html

## License Compliance

All dependencies have been reviewed and are suitable for commercial use. The licenses require:

1. **Attribution**: Copyright notices and license texts must be preserved
2. **License Distribution**: License files should be included when distributing the software
3. **No Warranty**: Software is provided "as is" without warranty

## Notes

- This file is auto-generated during the build process
- For the most up-to-date list including transitive dependencies, see `build/reports/sbom.json`
- All license texts are available in the respective library JAR files under `META-INF/LICENSE*`

---

**Generated by:** Gradle dependency report task
**Build Date:** 2026-04-28 10:19:14
