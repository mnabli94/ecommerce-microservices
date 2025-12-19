FROM maven:3.9.8-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml ./pom.xml
RUN mvn -q -DskipTests install -N

COPY api-gateway/pom.xml api-gateway/pom.xml
COPY api-gateway/src api-gateway/src
RUN mvn -q -DskipTests -f api-gateway/pom.xml package

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/api-gateway/target/*.jar app.jar
EXPOSE 4000
ENTRYPOINT ["java","-jar","/app/app.jar"]