FROM amazoncorretto:21

WORKDIR /app
ADD target/rinhabackend.jar .
CMD ["java", "-XX:MaxRAMPercentage=75", "-jar", "rinhabackend.jar"]
