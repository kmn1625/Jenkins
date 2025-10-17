# Multi-stage build for Java Spring Boot Calculator Application

# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder

WORKDIR /app

# Copy pom.xml first for dependency caching
COPY pom.xml .

# Download dependencies
RUN mvn dependency:go-offline -B || true

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine

# Create non-root user
RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app

# Copy the JAR file from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Change ownership
RUN chown -R spring:spring /app

USER spring

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
