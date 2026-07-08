FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml ./
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B verify

FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S app && adduser -S -G app app

WORKDIR /app

COPY --from=build /workspace/target/order-worker-0.0.1-SNAPSHOT.jar app.jar

USER app
EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=5s --start-period=20s --retries=6 \
    CMD wget -q -O - http://127.0.0.1:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
