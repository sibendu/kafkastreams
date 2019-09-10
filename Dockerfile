FROM openjdk:8
 
COPY target/streams.examples-0.1.jar streams.examples-0.1.jar

CMD ["java -jar streams.examples-0.1.jar"] 