#!/bin/bash

JAVA_VERSION=21
NODE_MAJOR=20

# Build/Update 'openems-build' Container for Drone/Woodpecker CI

docker pull eclipse-temurin:${JAVA_VERSION}-jdk

docker build -t openems-build:${JAVA_VERSION}.${NODE_MAJOR} -f - . <<EOF
FROM eclipse-temurin:${JAVA_VERSION}-jdk

RUN curl -fsSL https://deb.nodesource.com/setup_${NODE_MAJOR}.x | bash -

RUN apt-get update  \
        && apt-get install --no-install-recommends -y git nodejs wget unzip xz-utils;

# Source: https://stackoverflow.com/questions/62588767/running-google-chrome-on-wsl-ubuntu-as-headless-no-sandbox-gives-multiple
RUN wget https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb \
        && apt-get install --no-install-recommends -y ./google-chrome-stable_current_amd64.deb \
        && rm google-chrome-stable_current_amd64.deb;

RUN npm install -g @angular/cli

RUN apt-get clean && rm -rf /var/lib/apt/lists/*

RUN ln -sf /bin/bash /bin/sh

EOF