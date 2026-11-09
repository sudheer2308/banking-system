# payment-service/Dockerfile

# step 1: use java 17 as base image
FROM eclipse-temurin:17-jre-alpine

# Step 2: set working directory
WORKDIR /app

# Step 3: copy the build jar file
COPY target/payment-service-0.0.1-SNAPSHOT.jar app.jar

# step 4 : Expose port
EXPOSE 8083

# step 5: RUN the jar
# docker build -t transaction-service:latest .
# run the docker file -> docker run -p 8082:8082 --network banking-network transaction-service:latest
ENTRYPOINT ["java", "-jar", "app.jar"]