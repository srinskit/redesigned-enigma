from maven:latest

WORKDIR /usr/share/app
COPY pom.xml .
RUN mvn clean package
