FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml ./
COPY services ./services
RUN mvn -pl services/match-service -am package -DskipTests
RUN curl -L -o /build/opentelemetry-javaagent.jar https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v1.33.3/opentelemetry-javaagent.jar

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/services/match-service/target/*.jar /app/app.jar
COPY --from=build /build/opentelemetry-javaagent.jar /otel/opentelemetry-javaagent.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
