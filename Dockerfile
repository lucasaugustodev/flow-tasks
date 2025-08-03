# Single-stage build for Railway deployment
FROM maven:3.8.6-openjdk-11

# Install Node.js
RUN curl -fsSL https://deb.nodesource.com/setup_18.x | bash - && \
    apt-get install -y nodejs

WORKDIR /app

# Copy everything
COPY . .

# Build frontend
WORKDIR /app/frontend
RUN npm install
RUN npm run build

# Copy frontend build to Spring Boot static resources
WORKDIR /app
RUN mkdir -p src/main/resources/static
RUN cp -r frontend/dist/frontend/* src/main/resources/static/ || \
    cp -r frontend/dist/* src/main/resources/static/ || \
    echo "No frontend build found"

# Build Spring Boot application
RUN mvn clean package -DskipTests

# Expose port
EXPOSE 8080

# Set environment variables
ENV SPRING_PROFILES_ACTIVE=prod

# Run the application
CMD ["sh", "-c", "java -Dserver.port=${PORT:-8080} -jar target/project-management-system-1.0-SNAPSHOT.jar"]
