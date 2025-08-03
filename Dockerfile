# Multi-stage build optimized for Fly.io
FROM maven:3.8.6-openjdk-11 AS build

# Install Node.js
RUN curl -fsSL https://deb.nodesource.com/setup_18.x | bash - && \
    apt-get install -y nodejs

WORKDIR /app

# Copy package files first for better caching
COPY frontend/package*.json ./frontend/
COPY pom.xml ./

# Install frontend dependencies
WORKDIR /app/frontend
RUN npm ci

# Copy frontend source and build
COPY frontend/ ./
RUN npm run build

# Copy backend source
WORKDIR /app
COPY src/ ./src/

# Copy frontend build to Spring Boot static resources
RUN mkdir -p src/main/resources/static
RUN cp -r frontend/dist/frontend/* src/main/resources/static/ || \
    cp -r frontend/dist/* src/main/resources/static/ || \
    echo "No frontend build found"

# Build Spring Boot application
RUN mvn clean package -DskipTests

# Runtime stage
FROM openjdk:11-jre-slim

WORKDIR /app

# Copy the built JAR
COPY --from=build /app/target/project-management-system-1.0-SNAPSHOT.jar app.jar

# Create non-root user for security
RUN addgroup --system --gid 1001 appgroup && \
    adduser --system --uid 1001 --gid 1001 appuser

# Change ownership of the app directory
RUN chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8080

# Set environment variables
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-Xmx512m -Xms256m"

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/api/auth/health || exit 1

# Run the application
CMD ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT:-8080} -jar app.jar"]
