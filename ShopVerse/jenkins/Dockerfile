# Custom Jenkins image with Docker CLI + Node.js 20
# Demonstrates: Chapter 19 — CI/CD Pipeline with Jenkins
FROM jenkins/jenkins:lts-jdk21

USER root

# ── Install prerequisites ──────────────────────────────────────────────────
RUN apt-get update && apt-get install -y \
    ca-certificates \
    curl \
    gnupg \
    lsb-release \
    && rm -rf /var/lib/apt/lists/*

# ── Install Docker CLI (so Jenkins can run docker/docker compose commands) ──
RUN install -m 0755 -d /etc/apt/keyrings \
    && curl -fsSL https://download.docker.com/linux/debian/gpg \
       | gpg --dearmor -o /etc/apt/keyrings/docker.gpg \
    && chmod a+r /etc/apt/keyrings/docker.gpg \
    && echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
       https://download.docker.com/linux/debian \
       $(. /etc/os-release && echo "$VERSION_CODENAME") stable" \
       | tee /etc/apt/sources.list.d/docker.list > /dev/null \
    && apt-get update \
    && apt-get install -y docker-ce-cli docker-compose-plugin \
    && rm -rf /var/lib/apt/lists/*

# ── Install Node.js 20 (needed for npm ci / npm run build) ─────────────────
RUN curl -fsSL https://deb.nodesource.com/setup_20.x | bash - \
    && apt-get install -y nodejs \
    && rm -rf /var/lib/apt/lists/*

# ── Give jenkins user access to the Docker socket ──────────────────────────
# GID 999 is the standard docker group on most Linux hosts.
# If your host docker group GID differs, set DOCKER_GID build-arg:
#   docker compose build --build-arg DOCKER_GID=$(stat -c '%g' /var/run/docker.sock) jenkins
ARG DOCKER_GID=999
RUN groupadd -f -g ${DOCKER_GID} docker \
    && usermod -aG docker jenkins

USER jenkins

# Skip the initial setup wizard (admin password prompt) for convenience.
# Remove this line in a production Jenkins setup and set credentials properly.
ENV JAVA_OPTS="-Djenkins.install.runSetupWizard=false"
