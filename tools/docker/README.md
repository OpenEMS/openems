How to use openems docker images.

Start openems docker containers:

With docker compose:
1. Copy docker-compose.yml to a directory of your choice.

2. Typ the following command in the directory where the docker-compose.yml file is located.
docker compose up -d

3. Access openems in your browser.
IP = IP address of the docker host ("localhost" for local running docker)
For edge:
http://IP:8080/system/console/configMgr

For backend:
http://IP:8079/system/console/configMgr

For ui:
http://IP


Build your own docker image for edge, backend and ui:

1. Go into the root directory of the openems project.

2. Type the following build command.
For edge:
docker build -t openems_edge -f tools/docker/openems-edge/Dockerfile .

For backend:
docker build -t openems_backend -f tools/docker/openems-backend/Dockerfile .

For ui:
docker build -t openems_ui -f tools/docker/openems-backend/Dockerfile .