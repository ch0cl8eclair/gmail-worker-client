FROM gradle:8.10.0-jdk21 AS builder
WORKDIR /app
COPY ./src ./src
COPY ./gradlew ./
COPY ./build.gradle ./
COPY ./settings.gradle ./
COPY ./gradle/wrapper/*.* ./gradle/wrapper/
RUN ./gradlew clean build

FROM openjdk:21-jdk-slim
WORKDIR /app
EXPOSE 8888
COPY --from=builder /app/build/libs/gmail-worker-client-1.0.jar /app
ENTRYPOINT ["java", "-jar", "gmail-worker-client-1.0.jar"]
