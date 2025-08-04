ARG NODE=20
ARG VERSION

### Build ui files
FROM --platform=$BUILDPLATFORM node:${NODE}-alpine AS build_ui

ARG VERSION

RUN apk add --update --no-cache patch dos2unix

COPY ui/ /src/
COPY tools/docker/ui/index.patch /opt/index.patch

COPY tools/docker/ui/assets/ /opt/assets/
COPY tools/docker/ui/root/ /opt/root/

RUN find /opt/root/ -type f -name 'run' -exec chmod +x {} \;
RUN find /opt -type f -print0 | xargs -0 dos2unix

WORKDIR /src

RUN patch src/index.html < /opt/index.patch && \
    npm install && \
    node_modules/.bin/ng build -c "$VERSION";

### Build ui base
FROM ghcr.io/linuxserver/baseimage-alpine:edge AS ui_base

RUN apk add --update --no-cache \
    nginx openssl bash dos2unix rsync

### Build ui container
FROM ui_base

COPY --from=build_ui /src/target/ /var/www/html/openems/
COPY --from=build_ui /opt/assets/ /var/www/html/openems/assets/
COPY --from=build_ui /opt/root/ /

RUN find /etc/s6-overlay/ -type f -name 'run' -exec chmod +x {} \;

RUN find /etc/s6-overlay -type f -print0 | xargs -0 dos2unix && \
    find /etc/nginx -type f -print0 | xargs -0 dos2unix

VOLUME /etc/nginx
VOLUME /var/log/nginx

ENV UI_WEBSOCKET="ws://localhost:8085"

EXPOSE 80 443
