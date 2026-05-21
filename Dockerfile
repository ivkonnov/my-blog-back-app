FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY mvnw .
COPY pom.xml .
COPY .mvn .mvn
RUN chmod +x mvnw
COPY src ./src
RUN ./mvnw clean package -DskipTests

FROM tomcat:11.0.21-jdk21-temurin-jammy
RUN rm -rf /usr/local/tomcat/webapps/*
COPY --from=builder /app/target/my-blog-back-app.war /usr/local/tomcat/webapps/ROOT.war
EXPOSE 8080
CMD ["catalina.sh", "run"]
