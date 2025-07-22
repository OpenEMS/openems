ARG JAVA_VERSION=21

### Build edge binary
FROM --platform=$BUILDPLATFORM eclipse-temurin:${JAVA_VERSION}-alpine AS build_edge

RUN apk add --no-cache dos2unix bash

COPY tools/docker/edge/root/ /opt/root/

RUN find /opt/ -type f -print0 | xargs -0 dos2unix
RUN find /opt/ -type f -name 'run' -o -name '*.sh' -exec chmod +x {} \;

WORKDIR /src

ENV OEMS_EDGE_OUTPUT=/opt/openems-edge.jar
RUN --mount=type=bind,target=.,readwrite \
    --mount=type=cache,target=/root/.gradle \
        ./gradlew --no-build-cache buildEdge

### Build jar container base
FROM ghcr.io/linuxserver/baseimage-alpine:edge AS base_container

ARG JAVA_VERSION

RUN wget -O /etc/apk/keys/adoptium.rsa.pub https://packages.adoptium.net/artifactory/api/security/keypair/public/repositories/apk && \
    echo 'https://packages.adoptium.net/artifactory/apk/alpine/main' >> /etc/apk/repositories

RUN apk add --update --no-cache \
    temurin-${JAVA_VERSION}-jre rsync

### Build edge container
FROM base_container

COPY --from=build_edge /opt/root/ /
COPY --from=build_edge /opt/openems-edge.jar /opt/openems/openems-edge.jar

RUN find /etc/s6-overlay/ -type f -name 'run' -exec chmod +x {} \;

RUN find /etc/s6-overlay -type f -print0 | xargs -0 dos2unix && \
    find /var/lib/openems-default-config -type f -print0 | xargs -0 dos2unix

VOLUME /var/opt/openems/config
VOLUME /var/opt/openems/data

EXPOSE 8080 8085