# api-gateway/Dockerfile

# step 1: use java 17 as base image
FROM eclipse-temurin:17-jre-alpine

# Step 2: set working directory
WORKDIR /app

# Step 3: copy the build jar file
COPY target/api-gateway-0.0.1-SNAPSHOT.jar app.jar

# step 4 : Expose port
EXPOSE 8080

# step 5: RUN the jar
# docker build -t api-gateway:latest .
# run the docker file -> docker run -p 8080:8080 --network banking-network api-gateway:latest
ENTRYPOINT ["java", "-jar", "app.jar"]