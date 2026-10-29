# syntax=docker/dockerfile:1.7
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY apps/api/pom.xml pom.xml
COPY apps/api/src src
RUN --mount=type=cache,target=/root/.m2 mvn -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 aether && mkdir -p /var/lib/aether && chown aether /var/lib/aether
COPY --from=build /build/target/aether-api.jar /app/aether-api.jar
ENV AETHER_API_HOST=0.0.0.0 AETHER_API_PORT=38472 AETHER_DB_URL=jdbc:sqlite:/var/lib/aether/aether.db
USER aether
VOLUME ["/var/lib/aether"]
EXPOSE 38472
ENTRYPOINT ["java", "-XX:TieredStopAtLevel=1", "-jar", "/app/aether-api.jar"]
