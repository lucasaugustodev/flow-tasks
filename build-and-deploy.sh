#!/bin/bash

echo "=== Building Frontend Locally ==="

# Navigate to frontend directory
cd frontend || exit 1

# Install dependencies
echo "Installing dependencies..."
npm install

# Build Angular app
echo "Building Angular application..."
npm run build

# Check if build was successful
if [ $? -eq 0 ]; then
    echo "Frontend build successful!"
    
    # Create static directory
    mkdir -p ../src/main/resources/static
    
    # Copy built files to Spring Boot static resources
    echo "Copying built files to Spring Boot static resources..."
    if [ -d "dist/frontend" ]; then
        cp -r dist/frontend/* ../src/main/resources/static/
        echo "Files copied from dist/frontend/"
    elif [ -d "dist" ]; then
        cp -r dist/* ../src/main/resources/static/
        echo "Files copied from dist/"
    else
        echo "No dist directory found!"
        exit 1
    fi
    
    # Go back to root
    cd ..
    
    # Commit the built files
    echo "Committing built frontend files..."
    git add src/main/resources/static/
    git commit -m "Add built frontend files for Railway deployment"
    git push
    
    echo "Frontend build and commit completed successfully!"
    echo "Now you can deploy to Railway - it will only need to build the Spring Boot JAR"
else
    echo "Frontend build failed!"
    exit 1
fi
