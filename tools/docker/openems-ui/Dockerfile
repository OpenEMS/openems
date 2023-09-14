ARG UI_VERSION=openems,openems-backend-prod,prod

### Build ui builder
FROM --platform=$BUILDPLATFORM alpine:latest AS ui_builder

RUN apk update && apk upgrade

RUN apk add --no-cache \
    npm \
    nodejs

RUN npm install -g @angular/cli


### Build ui files
FROM --platform=$BUILDPLATFORM ui_builder AS build_ui

ARG UI_VERSION

COPY ./ /src
COPY tools/docker/openems-ui/src /src

WORKDIR /src/ui
RUN npm install
RUN ng build -c "${UI_VERSION}"


### Build ui base
FROM ghcr.io/linuxserver/baseimage-alpine:edge AS ui_base

RUN apk update && apk upgrade

RUN apk add --no-cache \
    nginx \
    openssl
	
## Build ui container
FROM ui_base

COPY --from=build_ui /src/ui/target /app/www
COPY tools/docker/openems-ui/root/ /

RUN mkdir -p /config /log

VOLUME /config
VOLUME /log

EXPOSE 80 443
