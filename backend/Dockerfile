# Aquí compilo el JAR en una imagen con Gradle para que el runtime no incluya herramientas de construcción.
FROM gradle:8.12.1-jdk21 AS build
WORKDIR /workspace
COPY build.gradle.kts settings.gradle.kts ./
COPY src ./src
RUN gradle --no-daemon bootJar

# Aquí ejecuto con un usuario sin privilegios y una imagen JRE pequeña para Cloud Run.
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 checkout
COPY --from=build /workspace/build/libs/*.jar /app/app.jar
USER checkout
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
