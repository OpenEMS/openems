ARG JAVA_VERSION=openjdk17

### Build jar builder
FROM --platform=$BUILDPLATFORM alpine:latest AS jar_builder

ARG JAVA_VERSION

RUN apk update && apk upgrade

RUN apk add --no-cache \
    ${JAVA_VERSION}-jdk


### Build edge binary
FROM --platform=$BUILDPLATFORM jar_builder AS build_edge

WORKDIR /src
RUN --mount=type=bind,target=.,readwrite \
    --mount=type=cache,target=/root/.gradle \
        ./gradlew --build-cache buildEdge && \
        cp build/* /opt


### Build jar container base
FROM ghcr.io/linuxserver/baseimage-alpine:edge AS base_container

ARG JAVA_VERSION

RUN apk update && apk upgrade

RUN apk add --no-cache \
    ${JAVA_VERSION}-jre-headless


### Build edge container
FROM base_container

COPY --from=build_edge /opt /app
COPY tools/docker/openems-edge/root/ /

RUN mkdir -p /config /data

VOLUME /config
VOLUME /data

EXPOSE 8080