ARG JAVA_VERSION=21

### Build backend binary
FROM --platform=$BUILDPLATFORM eclipse-temurin:${JAVA_VERSION}-alpine AS build_backend

WORKDIR /src
RUN --mount=type=bind,target=.,readwrite \
    --mount=type=cache,target=/root/.gradle \
        ./gradlew --no-build-cache assemble && \
        ./gradlew --no-build-cache buildBackend && \
        cp build/openems-backend.jar /opt

### Build jar container base
FROM ghcr.io/linuxserver/baseimage-alpine:edge AS base_container

ARG JAVA_VERSION

RUN wget -O /etc/apk/keys/adoptium.rsa.pub https://packages.adoptium.net/artifactory/api/security/keypair/public/repositories/apk  && \
    echo 'https://packages.adoptium.net/artifactory/apk/alpine/main' >> /etc/apk/repositories

RUN apk update && apk upgrade

RUN apk add --no-cache \
    temurin-${JAVA_VERSION}-jre

### Build backend container
FROM base_container

RUN mkdir -p /opt/openems /var/opt/openems/config /var/opt/openems/data

COPY --from=build_backend /opt /opt/openems
COPY tools/docker/backend/root/ /

RUN find /etc/s6-overlay/s6-rc.d -type f -exec chmod +x {} \;

VOLUME /var/opt/openems/config
VOLUME /var/opt/openems/data

EXPOSE 8079 8081 8082