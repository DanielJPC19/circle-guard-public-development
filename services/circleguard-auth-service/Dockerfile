FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle/ gradle/
COPY services/circleguard-auth-service/build.gradle.kts services/circleguard-auth-service/

RUN chmod +x gradlew && \
    ./gradlew :services:circleguard-auth-service:dependencies --no-daemon --quiet

COPY services/circleguard-auth-service/src services/circleguard-auth-service/src

RUN ./gradlew :services:circleguard-auth-service:bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/services/circleguard-auth-service/build/libs/*.jar app.jar
EXPOSE 8180
ENTRYPOINT ["java", "-jar", "app.jar"]
