# Ch13-01: Multi-stage Docker build — builder + lean runtime image
# Stage 1: Build
FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /workspace

COPY . .
RUN ./mvnw -pl shopverse-web -am package -DskipTests -q

# Stage 2: Runtime — JRE-only, non-root
FROM eclipse-temurin:21-jre-jammy AS runtime

# Security: non-root user
RUN groupadd --system shopverse && useradd --system --gid shopverse shopverse

WORKDIR /app
COPY --from=builder /workspace/shopverse-web/target/*.jar app.jar

# Health-check
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

USER shopverse
EXPOSE 8080

# Ch13-xx: G1GC tuning, container-aware heap, virtual threads
ENTRYPOINT ["java", \
  "-XX:+UseG1GC", \
  "-XX:MaxGCPauseMillis=200", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
