#!/bin/bash

NODE_MAJOR=22
NPM_VERSION=10
JAVA_VERSION=21

ANDROID_SDK_VERSION=11076708
ANDROID_HOME="/opt/android-sdk"
ANDROID_BUILD_TOOLS_VERSION=34.0.0
ANDROID_PLATFORMS_VERSION=34

# Build/Update 'openems-android' Container for Drone/Woodpecker CI

docker pull node:${NODE_MAJOR}

docker build -t openems-android:${JAVA_VERSION}.${NODE_MAJOR}.${ANDROID_PLATFORMS_VERSION} -f - . <<EOF
FROM node:${NODE_MAJOR}

SHELL ["/bin/bash", "-c"]

RUN apt-get update  \
        && apt-get install --no-install-recommends -y git nodejs wget unzip android-sdk apt-transport-https gpg;

RUN wget -qO - https://packages.adoptium.net/artifactory/api/gpg/key/public | gpg --dearmor | tee /etc/apt/trusted.gpg.d/adoptium.gpg > /dev/null \
       && echo "deb https://packages.adoptium.net/artifactory/deb $(awk -F= '/^VERSION_CODENAME/{print$2}' /etc/os-release) main" | tee /etc/apt/sources.list.d/adoptium.list \
       && apt-get update && apt-get install --no-install-recommends -y temurin-${JAVA_VERSION}-jdk;

RUN npm install -g npm@${NPM_VERSION}
RUN npm install -g @angular/cli @ionic/cli @capacitor/cli

ENV ANDROID_HOME="${ANDROID_HOME}"

RUN wget https://dl.google.com/android/repository/commandlinetools-linux-${ANDROID_SDK_VERSION}_latest.zip \
    && unzip commandlinetools-linux-${ANDROID_SDK_VERSION}_latest.zip \
    && mkdir $ANDROID_HOME && mv cmdline-tools $ANDROID_HOME \
    && yes | $ANDROID_HOME/cmdline-tools/bin/sdkmanager --sdk_root=$ANDROID_HOME --licenses \
    && $ANDROID_HOME/cmdline-tools/bin/sdkmanager --sdk_root=$ANDROID_HOME "platform-tools" "build-tools;${ANDROID_BUILD_TOOLS_VERSION}" "platforms;android-${ANDROID_PLATFORMS_VERSION}"

ENV PATH="$PATH:$ANDROID_HOME/cmdline-tools/bin"

RUN apt-get clean && rm -rf /var/lib/apt/lists/*

RUN ln -sf /bin/bash /bin/sh

EOF

