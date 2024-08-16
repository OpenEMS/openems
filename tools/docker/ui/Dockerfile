ARG NODE=20
ARG VERSION

### Build ui files
FROM --platform=$BUILDPLATFORM node:${NODE}-alpine AS build_ui

ARG VERSION

RUN apk add --update --no-cache patch

COPY ui/ /src/
COPY tools/docker/ui/index.patch /src/

WORKDIR /src

RUN patch src/index.html < index.patch && \
    npm install && \
    node_modules/.bin/ng build -c "$VERSION";

### Build ui base
FROM ghcr.io/linuxserver/baseimage-alpine:edge AS ui_base

RUN apk add --update --no-cache \
    nginx openssl
	
### Build ui container
FROM ui_base

COPY --from=build_ui /src/target/ /var/www/html/openems/
COPY tools/docker/ui/assets/ /var/www/html/openems/assets/
COPY tools/docker/ui/root/ /

RUN find /etc/s6-overlay/s6-rc.d -type f -name 'run' -exec chmod +x {} \;

VOLUME /etc/nginx
VOLUME /var/log/nginx

ENV UI_WEBSOCKET ws://localhost:8080

EXPOSE 80 443
