# account-service/Dockerfile

# step 1: use java 17 as base image
FROM eclipse-temurin:17-jre-alpine

# Step 2: set working directory
WORKDIR /app

# Step 3: copy the build jar file
COPY target/account-service-0.0.1-SNAPSHOT.jar app.jar

# step 4 : Expose port
EXPOSE 8081

# step 5: RUN the jar
ENTRYPOINT ["java", "-jar", "app.jar"]