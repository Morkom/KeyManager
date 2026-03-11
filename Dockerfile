# Stage 1: Build the application using a JDK 25 build environment
FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /app

# Copy the POM and source code
COPY pom.xml .
COPY src ./src

# Build the application with detailed error logging
RUN mvn clean install -DskipTests -e

# Stage 2: Create the runtime image using a JRE 25 base
FROM eclipse-temurin:25-jre

WORKDIR /app

# Copy the JAR from the build stage
COPY --from=build /app/target/*.jar app.jar

# Create a directory for keystores
RUN mkdir ./keystores

# Expose the port the app runs on
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
