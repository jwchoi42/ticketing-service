# Build Stage
FROM bellsoft/liberica-openjdk-alpine:17 AS builder
WORKDIR /app
COPY . .
RUN chmod +x gradlew
RUN ./gradlew clean build -x test

# Run Stage
FROM bellsoft/liberica-openjre-alpine:17
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
EXPOSE 8080

