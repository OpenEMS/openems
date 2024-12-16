FROM openjdk:21

WORKDIR /app
COPY ./build/openems-edge.jar /app/openems-edge.jar

CMD ["java", "-jar", "openems-edge.jar"]