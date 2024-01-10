FROM node:20.2.0 as builder
COPY ui ui/
WORKDIR ui
RUN npm install && \
    npm install -g @angular/cli@15.2.8
RUN ng build -c "openems,openems-backend-prod-eks,prod"

FROM nginx:latest

RUN mkdir /usr/share/nginx/html/m
COPY --from=builder /ui/target /usr/share/nginx/html/m
RUN mv /usr/share/nginx/html/m/*  /usr/share/nginx/html && \
    rm -rf /usr/share/nginx/html/m

COPY tools/docker/openems-ui/nginx.conf /etc/nginx/nginx.conf
