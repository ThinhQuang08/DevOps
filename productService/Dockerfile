# build stage
FROM maven:3.5.3-jdk-8-alpine AS product_builder 

WORKDIR /app-build
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# STAGE 2: Tạo image chạy ứng dụng
FROM openjdk:8-jre-slim

# Cài đặt curl
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=product_builder /app-build/target/product-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]