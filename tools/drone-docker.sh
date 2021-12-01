#!/bin/bash
# Build/Update 'openems-build' Container for Drone CI

docker pull adoptopenjdk/openjdk11

docker build -t openems-build:latest -<<EOF
FROM adoptopenjdk/openjdk11
RUN curl -fsSL https://deb.nodesource.com/setup_16.x | bash -
RUN apt-get update  \
        && apt-get install -y git nodejs  \
        && rm -rf /var/lib/apt/lists/*
EOF
