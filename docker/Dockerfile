# syntax=docker/dockerfile:1

FROM openjdk:11-jre-slim-buster

COPY LedgerServer.jar /LedgerServer.jar

EXPOSE 6666

CMD ["java", "-jar", "/LedgerServer.jar"]
