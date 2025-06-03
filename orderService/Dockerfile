# build stage
FROM maven:3.5.3-jdk-8-alpine AS order_builder 

WORKDIR /app-build

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

#run stage
FROM openjdk:8-jre-slim
# cài curl
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

WORKDIR /app
EXPOSE 8082

COPY --from=order_builder /app-build/target/order-service-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]