FROM maven:3.9.8-eclipse-temurin-17 AS build
WORKDIR /app
ARG SERVICE

COPY pom.xml ./pom.xml
RUN mvn -q -DskipTests install -N

COPY events-api/pom.xml  events-api/pom.xml
COPY events-api/src events-api/src
RUN mvn -q -f events-api/pom.xml clean install

COPY kafka-utils/pom.xml kafka-utils/pom.xml
COPY kafka-utils/src kafka-utils/src
RUN mvn -q -f kafka-utils/pom.xml clean install

COPY security-utils/pom.xml security-utils/pom.xml
COPY security-utils/src security-utils/src
RUN mvn -q -f security-utils/pom.xml clean install

COPY ${SERVICE}/pom.xml ${SERVICE}/pom.xml
COPY ${SERVICE}/src ${SERVICE}/src
RUN mvn -q -DskipTests -f ${SERVICE}/pom.xml package

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
ARG SERVICE
COPY --from=build /app/${SERVICE}/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
