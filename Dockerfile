# Build stage
FROM eclipse-temurin:17-jdk AS builder

WORKDIR /app

# Copy Gradle wrapper and build config
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY gradle.properties gradle.properties

# Download dependencies (cached unless build config changes)
RUN ./gradlew dependencies --no-daemon || true

# Copy source and build
COPY src src

RUN ./gradlew bootJar --no-daemon -x test

# Runtime stage (Debian-based for multi-arch: amd64, arm64)
FROM eclipse-temurin:17-jre

# Install curl for healthcheck
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Add a non-root user for security
RUN groupadd -g 1000 app && useradd -u 1000 -g app -s /bin/sh -m app

WORKDIR /app

# Copy JAR from builder
COPY --from=builder /app/build/libs/*.jar app.jar

# JVM options for containers (adjust -Xmx based on ECS task memory)
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseContainerSupport"

USER app

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
