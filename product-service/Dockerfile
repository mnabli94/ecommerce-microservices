FROM maven:3.9.8-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml ./pom.xml
RUN mvn -q install -N
COPY kafka-utils/pom.xml kafka-utils/pom.xml
COPY kafka-utils/src kafka-utils/src
RUN mvn -q -DskipTests -f kafka-utils/pom.xml clean install

COPY product-service/pom.xml product-service/pom.xml
COPY product-service/src product-service/src
RUN mvn -q -DskipTests -f product-service -am package

# ---- Run ----
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/product-service/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java","-jar","/app/app.jar"]