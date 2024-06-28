#!/bin/bash

NODE_MAJOR=20

ANDROID_SDK_VERSION=9477386
ANDROID_HOME="/opt/android-sdk"
ANDROID_BUILD_TOOLS_VERSION=32.0.0
ANDROID_PLATFORMS_VERSION=32

# Build/Update 'openems-android' Container for Drone/Woodpecker CI

docker pull node:${NODE_MAJOR}

docker build -t openems-android:${NODE_MAJOR}.${ANDROID_PLATFORMS_VERSION} -f - . <<EOF
FROM node:${NODE_MAJOR}

RUN apt-get update  \
        && apt-get install --no-install-recommends -y git nodejs wget unzip android-sdk;

RUN npm install -g @angular/cli @ionic/cli @capacitor/cli

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

