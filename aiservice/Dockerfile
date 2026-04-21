FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jdk

WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Render will use this port
EXPOSE 8083

# ✅ Use the shell form to ensure environment variables are expanded correctly
# Use the actual values or the Render variables
ENTRYPOINT ["java", "-jar", "app.jar", \
"--spring.datasource.url=${SPRING_DATASOURCE_AI_URL}", \
"--spring.datasource.username=${SPRING_DATASOURCE_AI_USERNAME}", \
"--spring.datasource.password=${SPRING_DATASOURCE_AI_PASSWORD}", \
"--spring.datasource.driver-class-name=org.postgresql.Driver", \
"--server.port=8083"]