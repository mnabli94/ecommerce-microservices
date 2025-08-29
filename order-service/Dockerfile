# ---- Build ----
FROM maven:3.9.8-eclipse-temurin-17 AS build
WORKDIR /app
# contexte = racine du repo (important)
COPY pom.xml ./pom.xml
COPY order-service/pom.xml order-service/pom.xml
COPY order-service/src order-service/src
RUN mvn -q -DskipTests -f order-service -am package

# ---- Run ----
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/order-service/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java","-jar","/app/app.jar"]