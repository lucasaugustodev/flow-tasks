# Multi-stage build for Railway deployment

# Stage 1: Build Frontend
FROM node:18-alpine AS frontend-build

WORKDIR /app/frontend

# Copy frontend package files
COPY frontend/package*.json ./

# Install dependencies
RUN npm ci --only=production

# Copy frontend source
COPY frontend/ ./

# Build Angular app
RUN npm run build

# Stage 2: Build Backend
FROM maven:3.8.6-openjdk-11 AS backend-build

WORKDIR /app

# Copy pom.xml first for dependency caching
COPY pom.xml ./

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy source code
COPY src/ ./src/

# Copy built frontend from previous stage
COPY --from=frontend-build /app/frontend/dist/frontend/ ./src/main/resources/static/

# Build Spring Boot application
RUN mvn clean package -DskipTests

# Stage 3: Runtime
FROM openjdk:11-jre-slim

WORKDIR /app

# Copy the built JAR
COPY --from=backend-build /app/target/project-management-system-1.0-SNAPSHOT.jar app.jar

# Expose port
EXPOSE 8080

# Set environment variables
ENV SPRING_PROFILES_ACTIVE=prod

# Run the application
CMD ["java", "-Dserver.port=${PORT:-8080}", "-jar", "app.jar"]
