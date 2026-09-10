# ================================
# Stage 1: Build the Spring Boot app
# ================================

FROM eclipse-temurin:17-jdk AS build

WORKDIR /app

# Copy Maven wrapper and project files
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Make Maven wrapper executable
RUN chmod +x mvnw

# Download dependencies first
RUN ./mvnw dependency:go-offline -DskipTests

# Copy source code
COPY src src

# Build the application
RUN ./mvnw clean package -DskipTests


# ================================
# Stage 2: Run the application
# ================================

FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy the generated Spring Boot JAR
COPY --from=build /app/target/demo-0.0.1-SNAPSHOT.jar app.jar

# Render provides the PORT environment variable
EXPOSE 8080

# Start Spring Boot
ENTRYPOINT ["java", "-jar", "app.jar"]