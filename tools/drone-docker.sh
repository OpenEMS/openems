#!/bin/bash

JAVA_VERSION=21

NODE_MAJOR=20

ANDROID_SDK_VERSION=9477386
ANDROID_HOME="/opt/android-sdk"
ANDROID_BUILD_TOOLS_VERSION=32.0.0
ANDROID_PLATFORMS_VERSION=32


# Build/Update 'openems-build' Container for Drone/Woodpecker CI

docker pull eclipse-temurin:${JAVA_VERSION}-jdk

docker build -t openems-build:${JAVA_VERSION}.${NODE_MAJOR} -f - . <<EOF
FROM eclipse-temurin:${JAVA_VERSION}-jdk

RUN curl -fsSL https://deb.nodesource.com/setup_${NODE_MAJOR}.x | bash -

RUN apt-get update  \
        && apt-get install --no-install-recommends -y git nodejs wget unzip android-sdk;

RUN npm install -g @angular/cli @ionic/cli @capacitor/cli

# Source: https://stackoverflow.com/questions/62588767/running-google-chrome-on-wsl-ubuntu-as-headless-no-sandbox-gives-multiple
RUN wget https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb \
        && apt-get install --no-install-recommends -y ./google-chrome-stable_current_amd64.deb \
        && rm google-chrome-stable_current_amd64.deb;

ENV ANDROID_HOME="${ANDROID_HOME}"

RUN wget https://dl.google.com/android/repository/commandlinetools-linux-${ANDROID_SDK_VERSION}_latest.zip \
    && unzip commandlinetools-linux-${ANDROID_SDK_VERSION}_latest.zip \
    && mkdir $ANDROID_HOME && mv cmdline-tools $ANDROID_HOME \
    && yes | $ANDROID_HOME/cmdline-tools/bin/sdkmanager --sdk_root=$ANDROID_HOME --licenses \
    && $ANDROID_HOME/cmdline-tools/bin/sdkmanager --sdk_root=$ANDROID_HOME "platform-tools" "build-tools;${ANDROID_BUILD_TOOLS_VERSION}" "platforms;android-${ANDROID_PLATFORMS_VERSION}"

ENV PATH "$PATH:$ANDROID_HOME/cmdline-tools/bin"

RUN apt-get clean && rm -rf /var/lib/apt/lists/*

RUN ln -sf /bin/bash /bin/sh

EOF

