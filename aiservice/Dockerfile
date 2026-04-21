FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jdk

WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Standard port for your service
EXPOSE 8083

# We use 'sh -c' to ensure the environment variables from Render are properly
# expanded into the command line without adding illegal quotes or spaces.
ENTRYPOINT ["sh", "-c", "java -jar app.jar --spring.datasource.url=${SPRING_DATASOURCE_AI_URL} --spring.datasource.username=${SPRING_DATASOURCE_AI_USERNAME} --spring.datasource.password=${SPRING_DATASOURCE_AI_PASSWORD} --spring.datasource.driver-class-name=org.postgresql.Driver --server.port=8083"]