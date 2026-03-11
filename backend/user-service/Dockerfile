FROM eclipse-temurin:25-jre-alpine AS builder
WORKDIR /builder
COPY target/user-service-0.0.1-SNAPSHOT.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract --destination extracted

FROM eclipse-temurin:25-jre-alpine
RUN addgroup -S finpay && adduser -S finpay -G finpay
WORKDIR /app
COPY --from=builder /builder/extracted/dependencies/ ./
COPY --from=builder /builder/extracted/spring-boot-loader/ ./
COPY --from=builder /builder/extracted/snapshot-dependencies/ ./
COPY --from=builder /builder/extracted/application/ ./
RUN chown -R finpay:finpay /app
USER finpay
EXPOSE 8082
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
