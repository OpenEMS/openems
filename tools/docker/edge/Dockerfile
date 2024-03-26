ARG JAVA_VERSION=21

### Build edge binary
FROM --platform=$BUILDPLATFORM eclipse-temurin:${JAVA_VERSION}-alpine AS build_edge

WORKDIR /src
RUN --mount=type=bind,target=.,readwrite \
    --mount=type=cache,target=/root/.gradle \
        ./gradlew --no-build-cache assemble && \
        ./gradlew --no-build-cache buildEdge && \
        cp build/openems-edge.jar /opt/

### Build jar container base
FROM ghcr.io/linuxserver/baseimage-alpine:edge AS base_container

ARG JAVA_VERSION

RUN apk update && apk upgrade

RUN apk add --no-cache \
    openjdk${JAVA_VERSION}-jre

### Build edge container
FROM base_container

COPY --from=build_edge /opt/ /opt/openems/
COPY tools/docker/edge/root/ /

RUN find /etc/s6-overlay/s6-rc.d -type f -name 'run' -exec chmod +x {} \;

VOLUME /var/opt/openems/config
VOLUME /var/opt/openems/data

EXPOSE 8080 8085