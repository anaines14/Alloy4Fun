@echo off

REM Step 1: Build the application using Maven
echo Building the application using Maven...
call .\mvnw package

REM Step 2: Build the Docker image
echo Building the Docker image...
call docker build -f src/main/docker/Dockerfile.jvm -t quarkus/alloy4fun-api-jvm .
