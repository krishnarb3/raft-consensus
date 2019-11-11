FROM java:8-jre

ENV VERTICLE_FILE raft-consensus-vertx-impl-1.0-SNAPSHOT-fat.jar

ENV VERTICLE_HOME /usr/verticles

EXPOSE 20000

COPY build/libs/$VERTICLE_FILE $VERTICLE_HOME/

WORKDIR $VERTICLE_HOME
ENTRYPOINT ["sh", "-c"]
CMD ["exec java -jar $VERTICLE_FILE"]