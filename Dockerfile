FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY gradle ./gradle
COPY build.gradle.kts settings.gradle.kts .
COPY gradlew .
RUN chmod +x gradlew
RUN ./gradlew --no-daemon dependencies
COPY src ./src
RUN ./gradlew --no-daemon bootJar

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# Копируем только готовый JAR-файл
COPY --from=builder /app/build/libs/blog-back-app.jar blog-back-app.jar
# Открываем порт и запускаем
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "blog-back-app.jar"]
