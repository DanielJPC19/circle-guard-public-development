FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle/ gradle/
COPY services/circleguard-notification-service/build.gradle.kts services/circleguard-notification-service/

RUN chmod +x gradlew && \
    ./gradlew :services:circleguard-notification-service:dependencies --no-daemon --quiet

COPY services/circleguard-notification-service/src services/circleguard-notification-service/src

RUN ./gradlew :services:circleguard-notification-service:bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/services/circleguard-notification-service/build/libs/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
