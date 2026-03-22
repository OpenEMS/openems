# How to use OpenEMS BackendEdge docker image:

- [How to use OpenEMS BackendEdge docker image:](#how-to-use-openems-backendedge-docker-image)
  - [Start openems docker containers](#start-openems-docker-containers)
    - [With docker compose](#with-docker-compose)
  - [Build your own OpenEMS BackendEdge docker image](#build-your-own-openems-backendedge-docker-image)
  - [Common Problems and Solutions](#common-problems-and-solutions)

## Start openems docker containers

### With docker compose

1. **Copy [docker-compose.yml](./docker-compose.yml) to a directory of your choice.**

2. **Typ the following command in the directory where the [docker-compose.yml](./docker-compose.yml) file is located.**

    ```bash
    docker compose up -d
    ```

2. **Access OpenEMS in your browser.**

    |              |                                                   |
    | ------------ | ------------------------------------------------- |
    | Apache Felix | http://\<hostname\>:8078/system/console/configMgr |

*change `<hostname>` to the actual hostname*

## Build your own OpenEMS BackendEdge docker image

1. **Go into the root directory of the OpenEMS project.**

2. **View or Change [Dockerfile](./Dockerfile)**

3. **Type the following build command.**

    ```bash
    docker build . -t openems_backend-edge -f tools/docker/backend-edge/Dockerfile
    ```

## Common Problems and Solutions

```bash
ERROR: failed to solve: error from sender: context canceled
```

When building the Docker image this error may occur because another program is accessing the project files. Try closing these programs (e.g. Eclipse IDE) and run the build command again.
