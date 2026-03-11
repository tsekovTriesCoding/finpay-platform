# Multi-stage Dockerfile for FinPay Spring Boot services
# Usage: docker build --build-arg SERVICE_NAME=auth-service -f backend/Dockerfile backend/auth-service

FROM eclipse-temurin:25-jre-alpine

ARG SERVICE_NAME
ENV SERVICE_NAME=${SERVICE_NAME}

RUN addgroup -S finpay && adduser -S finpay -G finpay

WORKDIR /app

COPY target/${SERVICE_NAME}-0.0.1-SNAPSHOT.jar app.jar

RUN chown -R finpay:finpay /app
USER finpay

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
