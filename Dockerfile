FROM openjdk:8u292
RUN mkdir -p /app /data
ADD target/botdarr-release.jar /app/botdarr-release.jar

#all mutable state (config/, database/, logs/) is created relative to /data so a
#single volume mount at /data persists everything; app files live in /app
WORKDIR /data

COPY ./docker/entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh

ENTRYPOINT ["/app/entrypoint.sh"]
CMD ["java", "-jar", "/app/botdarr-release.jar"]
