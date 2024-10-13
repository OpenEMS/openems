# How to use OpenEMS Edge docker image:

- [How to use OpenEMS Edge docker image:](#how-to-use-openems-edge-docker-image)
  - [Start openems docker containers](#start-openems-docker-containers)
    - [With docker compose](#with-docker-compose)
  - [Build your own OpenEMS Edge docker image](#build-your-own-openems-edge-docker-image)

## Start openems docker containers

### With docker compose
1. **Copy [docker-compose.yml](./docker-compose.yml) to a directory of your choice.**

2. **Typ the following command in the directory where the [docker-compose.yml](./docker-compose.yml) file is located.**
    
    ```bash
    docker compose up -d
    ```

3. **Access OpenEMS in your browser.**
   
    |       |                                                |
    | ----- | ---------------------------------------------- |
    | Edge: | http://localhost:8080/system/console/configMgr |
    | UI    | http://localhost:80/                           |

## Build your own OpenEMS Edge docker image

1. **Go into the root directory of the OpenEMS project.**

2. **View or Change [Dockerfile](./Dockerfile)**

3. **Type the following build commands.**
   
    ```bash
    docker build . -t openems_edge -f tools/docker/edge/Dockerfile
    ```

    *for UI Image see [ui/README.md](../ui/README.md)*

# Common Problems and Solutions
```
ERROR: failed to solve: error from sender: context canceled
```
When building the Docker image this error may occur because another program is accessing the project files. Try closing these programs (e.g. Eclipse IDE) and run the build command again.

