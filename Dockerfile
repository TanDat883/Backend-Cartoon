# Build stage
#FROM gradle:jdk21-corretto AS build
FROM gradle:jdk17-corretto AS build
WORKDIR /app

# Copy cấu hình Gradle để tận dụng cache
COPY build.gradle settings.gradle gradlew /app/
COPY gradle /app/gradle

# Copy mã nguồn và file .env
COPY src /app/src
#COPY .env /app/.env

# Build ứng dụng (bỏ qua test)
RUN ./gradlew build --no-daemon -x test

# Run stage
#FROM openjdk:21-jdk-slim
#FROM openjdk:17-jdk-slim
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app

# Copy file JAR từ build stage
#COPY --from=build /app/build/libs/*.jar app.jar
COPY --from=build /app/build/libs/*SNAPSHOT.jar app.jar
# Copy file .env từ build stage
#COPY --from=build /app/.env .env

# Expose port và chạy ứng dụng
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
