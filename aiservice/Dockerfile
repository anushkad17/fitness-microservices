FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jdk

WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Render needs to see this
EXPOSE 8083

# ✅ SHELL FORM: No square brackets!
# This forces the environment variables to actually expand.
ENTRYPOINT java -jar app.jar \
  --spring.datasource.url=${SPRING_DATASOURCE_AI_URL} \
  --spring.datasource.username=${SPRING_DATASOURCE_AI_USERNAME} \
  --spring.datasource.password=${SPRING_DATASOURCE_AI_PASSWORD} \
  --spring.cloud.config.enabled=false \
  --server.port=8083