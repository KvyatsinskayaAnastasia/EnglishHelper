FROM openjdk:17-jdk-alpine
ADD . /src
WORKDIR /src
COPY src ./src
RUN ./mvnw clean
RUN ./mvnw package -DskipTests
ENTRYPOINT ["java","-jar","target/english-0.0.1.jar"]