# Use a Gradle image with JDK 8 as the base image
FROM gradle:8 AS build

# Set the working directory inside the container
WORKDIR /app

# Copy the project source files
COPY . /app

# Build the application
RUN gradle bootJar

# Use a lighter base image for the final build
FROM openjdk:21-slim

# Set the working directory inside the container
WORKDIR /app

# Copy the JAR file from the build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Expose the application port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]