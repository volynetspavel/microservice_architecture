# --- Build Stage ---
FROM maven:3.9.12-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -Dmaven.test.skip=true

# --- Runtime Stage ---
FROM eclipse-temurin:17-jre-alpine-3.23 AS runtime
WORKDIR /app
COPY --from=build /app/target/*.jar resource-service.jar

# Expose port (change as needed)
EXPOSE 8081

# Run the application
CMD ["java", "-jar", "resource-service.jar"]