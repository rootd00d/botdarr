FROM openjdk:8u292
RUN mkdir -p /app /home/botdarr
ADD target/botdarr-release.jar /app/botdarr-release.jar

#all mutable state (config/, database/, logs/) is created relative to /home/botdarr
#(same location the official image uses) so a single volume mount there persists
#everything; app files live in /app, outside the mount
WORKDIR /home/botdarr

COPY ./docker/entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh

ENTRYPOINT ["/app/entrypoint.sh"]
CMD ["java", "-jar", "/app/botdarr-release.jar"]
