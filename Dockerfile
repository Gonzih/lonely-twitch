FROM java:8
COPY ./target /usr/src/twitch
WORKDIR /usr/src/twitch
CMD java $JVM_OPTS -cp lonely-twitch.jar clojure.main -m lonely-twitch.server
