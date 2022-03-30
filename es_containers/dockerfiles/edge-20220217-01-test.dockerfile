FROM eclipse-temurin:11

EXPOSE 8080-8085:8080-8085

RUN mkdir /usr/lib/openems
RUN mkdir /etc/openems.d


COPY edge-20220217-0.0.jar /usr/lib/openems/openems.jar

CMD ["java", "-jar", "/usr/lib/openems/openems.jar"]

