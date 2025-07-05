# Etapa de build
FROM eclipse-temurin:24-jdk as builder

WORKDIR /app
COPY . .

RUN ./gradlew bootJar

# Etapa final: apenas runtime
FROM eclipse-temurin:24-jre

WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8181
ENTRYPOINT ["java", "-jar", "app.jar"]
