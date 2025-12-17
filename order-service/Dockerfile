# ---- Build ----
FROM maven:3.9.8-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml ./pom.xml
RUN mvn -q install -N

COPY kafka-utils/pom.xml kafka-utils/pom.xml
COPY kafka-utils/src kafka-utils/src
RUN mvn -q -f kafka-utils/pom.xml clean install

COPY order-service/pom.xml order-service/pom.xml
COPY order-service/src order-service/src
RUN mvn -q -f order-service -am package

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/order-service/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]