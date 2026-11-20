FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /app

COPY . .

RUN mvn -B -f supply-chain-common-operations/supply-chain-common-operations/pom.xml clean install -DskipTests

RUN mvn -B -f supplier-lifecycle-operations/pom.xml clean package -DskipTests

FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build /app/supplier-lifecycle-operations/target/*.jar app.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]
