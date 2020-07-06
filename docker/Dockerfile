ARG VERSION="0.0.1-SNAPSHOT"

FROM openjdk:11.0.7-jre-slim

ARG VERSION
ENV JAR="calc-${VERSION}-fat.jar"

COPY target/${JAR} /
COPY docker/run.sh /

ENTRYPOINT ["./run.sh"]
