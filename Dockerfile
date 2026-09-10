# Java 24 deve permanecer alinhado ao toolchain do build.gradle.
FROM eclipse-temurin:24-jdk AS builder

WORKDIR /app
COPY gradlew build.gradle settings.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies

COPY src ./src
# O diretório de build é novo: somente o bootJar será gerado.
RUN ./gradlew --no-daemon bootJar && cp build/libs/*.jar /app/app.jar

# Etapa final: apenas runtime
FROM eclipse-temurin:24-jre

WORKDIR /app
RUN groupadd --gid 10001 app && useradd --uid 10001 --gid app --no-create-home --shell /usr/sbin/nologin app
COPY --from=builder --chown=10001:10001 /app/app.jar ./app.jar

# Ponto inicial de dimensionamento; ajustar conforme RSS e carga reais.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=60.0 -XX:+ExitOnOutOfMemoryError" \
    SERVER_PORT=8181 \
    SERVER_SHUTDOWN=graceful \
    SPRING_LIFECYCLE_TIMEOUT_PER_SHUTDOWN_PHASE=30s

USER 10001:10001
EXPOSE 8181
STOPSIGNAL SIGTERM
ENTRYPOINT ["java", "-jar", "app.jar"]
