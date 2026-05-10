FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle/ gradle/
COPY services/circleguard-promotion-service/build.gradle.kts services/circleguard-promotion-service/

RUN chmod +x gradlew && \
    ./gradlew :services:circleguard-promotion-service:dependencies --no-daemon --quiet

COPY services/circleguard-promotion-service/src services/circleguard-promotion-service/src

RUN ./gradlew :services:circleguard-promotion-service:bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/services/circleguard-promotion-service/build/libs/*.jar app.jar
EXPOSE 8088
ENTRYPOINT ["java", "-jar", "app.jar"]
