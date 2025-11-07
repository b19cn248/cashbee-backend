# Multi-stage Dockerfile for CashBee Backend
# Stage 1: Build stage using Maven
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder

# Set working directory
WORKDIR /app

# Copy parent POM first (for dependency caching)
COPY pom.xml .

# Copy module POMs
COPY cashbee-common/pom.xml ./cashbee-common/
COPY cashbee-domain/pom.xml ./cashbee-domain/
COPY cashbee-infrastructure/pom.xml ./cashbee-infrastructure/
COPY cashbee-application/pom.xml ./cashbee-application/
COPY cashbee-presentation/pom.xml ./cashbee-presentation/

# Download dependencies (this layer will be cached if POMs don't change)
RUN mvn dependency:go-offline -B

# Copy all source code
COPY cashbee-common/src ./cashbee-common/src
COPY cashbee-domain/src ./cashbee-domain/src
COPY cashbee-infrastructure/src ./cashbee-infrastructure/src
COPY cashbee-application/src ./cashbee-application/src
COPY cashbee-presentation/src ./cashbee-presentation/src

# Build the application (skip tests for faster build)
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime stage
FROM eclipse-temurin:21-jre-alpine

# Set working directory
WORKDIR /app

# Create non-root user for security
RUN addgroup -g 1001 -S cashbee && \
    adduser -u 1001 -S cashbee -G cashbee

# Copy JAR from builder stage
COPY --from=builder /app/cashbee-presentation/target/*.jar app.jar

# Create logs directory
RUN mkdir -p /app/logs && \
    chown -R cashbee:cashbee /app

# Switch to non-root user
USER cashbee

# Expose application port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Set JVM options - Optimized for production with memory leak protection
ENV JAVA_OPTS="-Xms1g -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/app/logs/heap-dump.hprof -XX:+ExitOnOutOfMemoryError -XX:MaxMetaspaceSize=512m -XX:+UnlockExperimentalVMOptions -XX:+UseStringDeduplication"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
