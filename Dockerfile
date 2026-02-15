# ----------------------------
# STAGE 1: The Builder (Compiles the code)
# ----------------------------
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

# Copy only the dependency definition first (caching optimization)
COPY pom.xml .
# Download dependencies (this layer will be cached if pom.xml doesn't change)
RUN mvn dependency:go-offline

# Copy the actual source code
COPY src ./src

# Build the JAR (Skip tests to speed up)
RUN mvn clean package -DskipTests

# ----------------------------
# STAGE 2: The Runner (Runs the app)
# ----------------------------
# Use the same base image that worked for you before
FROM eclipse-temurin:21-jdk

LABEL maintainer="my-cloud-file-server"
VOLUME /app/data

WORKDIR /app

# Copy ONLY the built JAR from the 'builder' stage
COPY --from=builder /build/target/*.jar app.jar

EXPOSE 9080

# Run the app
ENTRYPOINT ["java", "-jar", "app.jar"]