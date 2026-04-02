FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY pom.xml ./
COPY src/main src/main
RUN apt-get update && apt-get install -y maven && mvn dependency:go-offline -B
RUN mvn package -DskipTests -B

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
