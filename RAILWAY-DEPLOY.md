# Railway Deployment Guide

## Project Structure
This is a full-stack application with:
- **Backend**: Spring Boot (Java) serving REST APIs
- **Frontend**: Angular SPA served as static files by Spring Boot
- **Database**: H2 in-memory database for production

## Deployment Configuration

### Build Process
1. **Frontend Build**: Maven uses frontend-maven-plugin to:
   - Install Node.js and npm
   - Install Angular dependencies
   - Build Angular app for production
   - Copy built files to Spring Boot static resources

2. **Backend Build**: Maven builds Spring Boot JAR with embedded frontend

### Environment Variables
Set these in Railway:
- `SPRING_PROFILES_ACTIVE=prod` (enables production configuration)
- `PORT` (automatically set by Railway)

### URLs
- **Application**: Your Railway app URL serves the Angular frontend
- **API**: Same URL + `/api/*` for REST endpoints
- **Health Check**: `/api/auth/health`

### Key Files
- `railway.json`: Railway deployment configuration
- `Procfile`: Alternative deployment command
- `pom.xml`: Maven build with frontend integration
- `src/main/java/com/projectmanagement/config/WebConfig.java`: SPA routing configuration
- `src/main/resources/application-prod.properties`: Production settings

### Features in Production
- Angular SPA with client-side routing
- REST API backend
- JWT authentication
- In-memory H2 database (resets on restart)
- Static file serving optimized
- CORS configured for same-origin

### Deployment Steps
1. Connect Railway to your GitHub repository
2. Railway will automatically detect Java project
3. Build process runs: `mvn clean package -DskipTests`
4. App starts with: `java -Dserver.port=$PORT -Dspring.profiles.active=prod -jar target/project-management-system-1.0-SNAPSHOT.jar`

### Troubleshooting
- Check Railway logs for build/runtime errors
- Verify frontend files are in `target/classes/static/`
- Ensure API calls use relative URLs (not localhost:8080)
- Check CORS configuration if frontend can't reach API
