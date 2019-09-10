FROM openjdk:8-jdk-alpine
VOLUME /tmp
ADD target/streams.examples-0.1.jar streams.examples-0.1.jar
ENV JAVA_OPTS=""
ENTRYPOINT [ "java", "-jar", "streams.examples-0.1.jar" ] 