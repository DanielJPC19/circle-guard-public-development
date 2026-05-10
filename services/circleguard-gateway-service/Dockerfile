FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle/ gradle/
COPY services/circleguard-gateway-service/build.gradle.kts services/circleguard-gateway-service/

RUN chmod +x gradlew && \
    ./gradlew :services:circleguard-gateway-service:dependencies --no-daemon --quiet

COPY services/circleguard-gateway-service/src services/circleguard-gateway-service/src

RUN ./gradlew :services:circleguard-gateway-service:bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/services/circleguard-gateway-service/build/libs/*.jar app.jar
EXPOSE 8087
ENTRYPOINT ["java", "-jar", "app.jar"]
