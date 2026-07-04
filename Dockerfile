FROM maven:3.9.6-amazoncorretto-21-al2023 AS build
COPY . .
RUN mvn clean package -Dmaven.test.skip=true

FROM amazoncorretto:21.0.2-alpine3.19
WORKDIR /app
COPY --from=build target/QuizAPI.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]