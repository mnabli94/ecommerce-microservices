# ---- Build ----
FROM maven:3.9.8-eclipse-temurin-17 AS build
WORKDIR /app
# contexte = racine du repo (important)
COPY pom.xml ./pom.xml
COPY product-service/pom.xml product-service/pom.xml
COPY product-service/src product-service/src
RUN mvn -q -DskipTests -pl product-service -am package

# ---- Run ----
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/product-service/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]