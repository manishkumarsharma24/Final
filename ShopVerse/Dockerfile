# Demonstrates: Chapter 16 — Multi-Stage Docker Build with Maven dependency caching
# ─────────────────────────────────────────────────────────────────────────────
# Stage 1: Dependency cache layer
# Copy all pom.xml files first so this layer is only invalidated when
# a pom.xml changes — not on every source file change.
# ─────────────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-jammy AS deps

WORKDIR /workspace

# Copy Maven wrapper
COPY mvnw .
COPY .mvn .mvn
RUN chmod +x mvnw

# Copy all pom.xml files preserving module structure
COPY pom.xml .
COPY shopverse-domain/pom.xml          shopverse-domain/pom.xml
COPY shopverse-application/pom.xml     shopverse-application/pom.xml
COPY shopverse-infrastructure/pom.xml  shopverse-infrastructure/pom.xml
COPY shopverse-web/pom.xml             shopverse-web/pom.xml
COPY shopverse-gateway/pom.xml         shopverse-gateway/pom.xml
COPY shopverse-e2e-tests/pom.xml       shopverse-e2e-tests/pom.xml
COPY benchmarks/pom.xml                benchmarks/pom.xml

# Download all dependencies — this layer is cached until a pom.xml changes
RUN ./mvnw dependency:go-offline -pl shopverse-web -am -q

# ─────────────────────────────────────────────────────────────────────────────
# Stage 2: Build
# ─────────────────────────────────────────────────────────────────────────────
FROM deps AS builder

# Copy all source code
COPY shopverse-domain/src          shopverse-domain/src
COPY shopverse-application/src     shopverse-application/src
COPY shopverse-infrastructure/src  shopverse-infrastructure/src
COPY shopverse-web/src             shopverse-web/src

# Build only the web module and its dependencies, skip tests
RUN ./mvnw -pl shopverse-web -am package -DskipTests -q

# ─────────────────────────────────────────────────────────────────────────────
# Stage 3: Runtime — JRE-only, non-root user
# ─────────────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy AS runtime

# Security: non-root user
RUN groupadd --system shopverse && useradd --system --gid shopverse shopverse

WORKDIR /app

# Create heap dump directory
RUN mkdir -p /dumps && chown shopverse:shopverse /dumps

COPY --from=builder /workspace/shopverse-web/target/*.jar app.jar

HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=5 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

USER shopverse
EXPOSE 8080

# Ch13-xx: G1GC tuning, container-aware heap, virtual threads
ENTRYPOINT ["java", \
  "-XX:+UseG1GC", \
  "-XX:MaxGCPauseMillis=200", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+HeapDumpOnOutOfMemoryError", \
  "-XX:HeapDumpPath=/dumps/", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
