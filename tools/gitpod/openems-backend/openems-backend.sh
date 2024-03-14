#!/bin/bash

/usr/bin/java -XX:+ExitOnOutOfMemoryError -Dfelix.cm.dir=/opt/openems-backend/config.d -Djava.util.concurrent.ForkJoinPool.common.parallelism=100 -jar /opt/openems-backend/openems-backend.jar
