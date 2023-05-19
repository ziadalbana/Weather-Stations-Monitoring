# Set the base image to use for the container
FROM openjdk:11-jre-slim

# Set the working directory inside the container
WORKDIR /app

# Copy the Spring Boot application to the container
COPY out/artifacts/Central_station_jar/Central-station.jar app.jar

# Set the command to run the application when the container starts
CMD ["java", "-jar", "app.jar"]