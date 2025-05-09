ARG JAVA_VERSION=21

### Build backend binary
FROM --platform=$BUILDPLATFORM eclipse-temurin:${JAVA_VERSION}-alpine AS build_backend

WORKDIR /src
ENV OEMS_BACKEND_OUTPUT=/opt/openems-backend.jar
RUN --mount=type=bind,target=.,readwrite \
    --mount=type=cache,target=/root/.gradle \
        ./gradlew --no-build-cache buildBackend

### Build jar container base
FROM ghcr.io/linuxserver/baseimage-alpine:edge AS base_container

ARG JAVA_VERSION

RUN apk update && apk upgrade

RUN apk add --no-cache \
    openjdk${JAVA_VERSION}-jre rsync dos2unix

### Build backend container
FROM base_container

COPY --from=build_backend /opt/ /opt/openems/
COPY tools/docker/backend/root/ /

RUN find /etc/s6-overlay/s6-rc.d -type f -name 'run' -exec chmod +x {} \;

RUN find /etc/s6-overlay -type f -print0 | xargs -0 dos2unix && \
    find /var/opt/openems -type f -print0 | xargs -0 dos2unix

VOLUME /var/opt/openems/config
VOLUME /var/opt/openems/data

EXPOSE 8079 8081 8082