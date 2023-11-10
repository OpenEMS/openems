ARG JAVA_VERSION=openjdk17

### Build jar builder
FROM --platform=$BUILDPLATFORM alpine:latest AS jar_builder

ARG JAVA_VERSION

RUN apk update && apk upgrade

RUN apk add --no-cache \
    ${JAVA_VERSION}-jdk


### Build backend binary
FROM --platform=$BUILDPLATFORM jar_builder AS build_backend

WORKDIR /src
RUN --mount=type=bind,target=.,readwrite \
    --mount=type=cache,target=/root/.gradle \
        ./gradlew --build-cache buildBackend && \
        cp build/* /opt


### Build jar container base
FROM ghcr.io/linuxserver/baseimage-alpine:edge AS base_container

ARG JAVA_VERSION

RUN apk update && apk upgrade

RUN apk add --no-cache \
    ${JAVA_VERSION}-jre-headless


### Build backend container
FROM base_container

COPY --from=build_backend /opt /app
COPY tools/docker/backend/openems/root/ /

RUN mkdir -p /config /data

VOLUME /config
VOLUME /data

EXPOSE 8079