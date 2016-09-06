FROM anapsix/alpine-java:jre8
MAINTAINER David Aparicio "david.aparicio@free.fr"

ENV USERNAME duser
ENV HOME /home/$USERNAME
ENV SHELL /bin/sh
#/bin/ash

ENV JAR voivi-0.0.2-SNAPSHOT-fat.jar

# Don't be root.
RUN adduser -h ${HOME} -D -s ${SHELL} -S ${USERNAME}
USER ${USERNAME}
WORKDIR ${HOME}

# Expose remote access ports.
EXPOSE 10101

COPY ./src/main/resources/config.json .
COPY ./docker/entrypoint.sh .
COPY ./target/${JAR} .

ENTRYPOINT ["sh", "-c"]
CMD ["java -jar ${JAR} -conf config.json"]
