FROM eclipse-temurin:21-jre-alpine

RUN apk add --no-cache dumb-init

LABEL maintainer="igor.khokhriakov@hzg.de"

ARG JAR_FILE
ADD target/${JAR_FILE} /app/bin/ss.jar
ADD etc /app/etc

RUN addgroup --system javauser && adduser -S -s /bin/false -G javauser javauser
RUN chown -R javauser /app

USER javauser
WORKDIR /app

ENV SS_CONFIG=/app/etc/StatusServer/conf/config.xml
ENV SS_PORT=9190

ENTRYPOINT ["/usr/bin/dumb-init", "--"]
CMD java -jar /app/bin/ss.jar $SS_CONFIG $SS_PORT
