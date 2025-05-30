# build stage
FROM maven:3.8.5-openjdk-8-slim AS product_builder 
WORKDIR /app-build
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

#run stage
FROM openjdk:8-jre-slim
WORKDIR /app

COPY --from=product_builder /app-build/target/product-service-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]