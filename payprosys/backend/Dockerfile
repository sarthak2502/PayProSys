# Build stage
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Avoid OOM and improve Docker build reliability
ENV MAVEN_OPTS="-Xmx512m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

COPY pom.xml .
COPY src ./src
RUN mvn package -Dmaven.test.skip=true -B

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# For Docker healthchecks (Compose waits until the API port accepts traffic)
RUN apk add --no-cache curl

RUN adduser -D -s /bin/sh appuser
USER appuser

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
