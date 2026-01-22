FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml ./
COPY services ./services
RUN mvn -pl services/indexer-worker -am package -DskipTests
RUN curl -L -o /build/opentelemetry-javaagent.jar https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v1.33.3/opentelemetry-javaagent.jar

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/services/indexer-worker/target/*.jar /app/app.jar
COPY --from=build /build/opentelemetry-javaagent.jar /otel/opentelemetry-javaagent.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
