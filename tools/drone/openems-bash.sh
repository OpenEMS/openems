#!/bin/bash

# Build/Update 'openems-bash' Container for Drone/Woodpecker CI

docker pull bash:latest

docker build -t openems-bash:latest -f - . <<EOF
FROM bash:latest

RUN apk update \
        && apk add --no-cache bash nodejs git wget curl unzip xz

# Add simple cmd to cp and create destination directory
RUN echo "mkdir -p \$(dirname \"\${@: -1}\") && cp \$@" > /usr/bin/copy \
        && chmod +x /usr/bin/copy

RUN ln -sf /bin/bash /bin/sh

EOF