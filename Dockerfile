# 1. Maven alap image a buildhez
FROM maven:3.9.4-eclipse-temurin-21 AS build

WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# 2. Futási környezet
FROM eclipse-temurin:21-jre
WORKDIR /app

# A JAR átvétele az előző lépésből
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
