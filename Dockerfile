FROM openjdk:11
ADD target/shop-application-0.0.1-SNAPSHOT.jar shop-application.jar
ENTRYPOINT ["java","-jar","shop-application.jar"]
EXPOSE 8080
FROM maven:3.9.9-amazoncorretto-23 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests
FROM amazoncorretto:23
WORKDIR /app
COPY --from=build /app/target/*.jar shop-application.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "shop-application.jar"]