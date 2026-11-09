# notification-service/Dockerfile

# step 1: use java 17 as base image
FROM eclipse-temurin:17-jre-alpine

# Step 2: set working directory
WORKDIR /app

# Step 3: copy the build jar file
COPY target/notification-service-0.0.1-SNAPSHOT.jar app.jar

# step 4 : Expose port
EXPOSE 8085

# step 5: RUN the jar
# docker build -t notification-service:latest .
# run the docker file -> docker run -p 8085:8085 --network banking-network notification-service:latest
ENTRYPOINT ["java", "-jar", "app.jar"]