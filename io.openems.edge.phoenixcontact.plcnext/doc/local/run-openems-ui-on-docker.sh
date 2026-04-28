#!/bin/sh
docker run -p 4400:4400 \
    -v $(pwd)/local/openems-ui/etc/nginx/conf.d:/etc/nginx/conf.d \
    -v $(pwd)/../../ui/target:/opt/plcnext/apps/openems-ui/html \
    -v $(pwd)/deploy/openems-ui/etc/nginx/sites-available:/etc/nginx/sites-available \
    -v $(pwd)/deploy/openems-ui/opt/plcnext/apps/openems-ui/nginx:/opt/plcnext/apps/openems-ui/nginx \
    nginx:mainline-alpine-slim