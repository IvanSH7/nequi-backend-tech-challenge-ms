FROM eclipse-temurin:25-jdk-alpine
WORKDIR /app

RUN addgroup -S nequigroup && adduser -S nequiuser -G nequigroup

ENV TZ=America/Bogota
ENV JAVA_OPTS=" -XX:+UseContainerSupport -XX:MaxRAMPercentage=70 -XX:+UseZGC -Djava.security.egd=file:/dev/./urandom"

COPY applications/app-service/build/libs/nequi-backend-tech-challenge-ms.jar app.jar
RUN chown -R nequiuser:nequigroup /app

USER nequiuser

EXPOSE 9090

ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS  -jar app.jar" ]