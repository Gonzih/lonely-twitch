FROM java:8
COPY ./target /usr/src/twitch
WORKDIR /usr/src/twitch
EXPOSE 3000
ENV CLIENT_ID iks0is28m281tc7gbov0ia4yyxgtbv8
CMD java $JVM_OPTS -cp lonely-twitch.jar clojure.main -m lonely-twitch.server
