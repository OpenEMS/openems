#!/bin/bash
# Build/Update 'openems-build' Container for Drone CI

docker pull adoptopenjdk/openjdk11

docker build -t openems-build:latest -<<EOF
FROM adoptopenjdk/openjdk11
RUN curl -fsSL https://deb.nodesource.com/setup_18.x | bash -

RUN apt-get update  \
        && apt-get install -y apt-utils git nodejs wget curl unzip xvfb libxi6 libgconf-2-4 fonts-liberation \

# Source: https://stackoverflow.com/questions/62588767/running-google-chrome-on-wsl-ubuntu-as-headless-no-sandbox-gives-multiple
RUN wget https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb \
        && dpkg -i ./google-chrome-stable_current_amd64.deb \
        || apt-get install -y -f \
        && rm google-chrome-stable_current_amd64.deb

RUN rm -rf /var/lib/apt/lists/*
EOF

