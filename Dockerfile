FROM openjdk:14

WORKDIR /app
COPY src ./src
VOLUME src ./src

