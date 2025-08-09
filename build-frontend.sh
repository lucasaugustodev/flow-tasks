#!/bin/bash

echo "=== Building Frontend ==="

# Set environment

# Navigate to frontend directory
cd frontend || exit 1

echo "Node version: $(node --version)"
echo "NPM version: $(npm --version)"

# Install dependencies
echo "Installing dependencies..."
npm install

# Build Angular app
echo "Building Angular application..."
npm run build

# Check if build was successful
if [ $? -eq 0 ]; then
    echo "Frontend build successful!"

    # Create target directory if it doesn't exist
    mkdir -p ../src/main/resources/static

    # Copy built files to Spring Boot static resources
    echo "Copying built files to Spring Boot static resources..."
    if [ -d "dist/frontend" ]; then
        cp -r dist/frontend/* ../src/main/resources/static/
        echo "Files copied from dist/frontend/"
    else
        echo "dist/frontend not found, checking other locations..."
        ls -la dist/
    fi

    echo "Frontend build and copy completed successfully!"
else
    echo "Frontend build failed!"
    exit 1
fi
