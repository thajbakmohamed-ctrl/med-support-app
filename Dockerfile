
# Java 17 runtime
FROM eclipse-temurin:17-jre

# Application directory inside the container
WORKDIR /app

# Copy the Spring Boot JAR
COPY target/med-support-0.0.1-SNAPSHOT.jar app.jar

# Spring Boot container port
EXPOSE 8081

# Start the application
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
