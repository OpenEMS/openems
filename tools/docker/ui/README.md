# How to use OpenEMS UI docker image

## Start openems docker containers

### With docker compose

see [backend/README.md](../backend/README.md) and [edge/README.md](../edge/README.md) respectively.

## Build your own OpenEMS UI image

1. **Go into the root directory of the OpenEMS project.**

2. **View or Change [Dockerfile](./Dockerfile)**

3. **Type the following build command.**
    
    *Edge*
    ```bash
    docker build . -t openems_ui-edge -f tools/docker/ui/Dockerfile --build-arg VERSION=openems,openems-edge-docker 
    ```
    ---
    *Backend*
    ```bash
    docker build . -t openems_ui-backend -f tools/docker/ui/Dockerfile --build-arg VERSION=openems,openems-backend-docker 
    ```