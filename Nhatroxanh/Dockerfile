<<<<<<< HEAD
# Multi-stage build cho Spring Boot
FROM eclipse-temurin:21-jdk AS build

# Cài đặt Maven
RUN apt-get update && apt-get install -y maven

# Thiết lập thư mục làm việc
WORKDIR /app

# Copy pom.xml và download dependencies trước
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code và build
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-jammy

# Thiết lập thư mục làm việc
WORKDIR /app

# Copy file jar từ build stage
COPY --from=build /app/target/*.jar app.jar

# Tạo user non-root để bảo mật
RUN addgroup --system spring && adduser --system spring --ingroup spring
USER spring:spring

# Expose port 8080
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Chạy ứng dụng
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/app.jar"]
=======
# FROM ubuntu:latest
# LABEL authors="admin"

# ENTRYPOINT ["top", "-b"]
>>>>>>> d0f8371a4ebc5516bc279082440241282949544a
