# How to use OpenEMS Backend docker image:

- [How to use OpenEMS Backend docker image:](#how-to-use-openems-backend-docker-image)
  - [Start openems docker containers](#start-openems-docker-containers)
    - [With docker compose](#with-docker-compose)
  - [Setup Container](#setup-container)
    - [Setup Timedata.InfluxDB](#setup-timedatainfluxdb)
  - [Build your own OpenEMS Backend docker image](#build-your-own-openems-backend-docker-image)


## Start openems docker containers

### With docker compose
1. **Copy [docker-compose.yml](./docker-compose.yml) to a directory of your choice.**

2. **Typ the following command in the directory where the [docker-compose.yml](./docker-compose.yml) file is located.**
    
    ```bash
    docker compose up -d
    ```

3. **Access OpenEMS in your browser.**
   
    |         |                                                |
    | ------- | ---------------------------------------------- |
    | Backend | http://localhost:8079/system/console/configMgr |
    | UI      | http://localhost:80/

## Setup Container
### Setup Timedata.InfluxDB

1. **Setup InfluxDB**
      
    ```bash
    docker exec openems_influxdb influx setup \
      --username openems \
      --password WKeuIhl0deIJjrjoY62M \
      --org openems.io \
      --bucket openems \
      --force
    ```

    ```bash
      docker exec openems_influxdb influx auth list
    ```

    *⚠️ Note down **Token***

2. **Open Backend [Apache-Felix](http://localhost:8079/system/console/configMgr).**

3. **Remove Timedata.Dummy**

4. **Add Timedata.InfluxDB**

    *⚠️ Values not specified can be left at their default values*

    | Timedata.InfluxDB |                              |
    | ----------------- | ---------------------------- |
    | Query Language    | INFLUX_QL                    |
    | URL               | http://openems_influxdb:8086 |
    | Org               | openems.io                   |
    | ApiKey            | *InfluxDB-Token*             |
    | Bucket            | openems                      |

----
*for further information see [OpenEMS docs](https://openems.github.io/openems.io/openems/latest/introduction.html)*

## Build your own OpenEMS Backend docker image

1. **Go into the root directory of the OpenEMS project.**

2. **View or Change [Dockerfile](./Dockerfile)**

3. **Type the following build command.**
   
    ```bash
    docker build . -t openems_backend -f tools/docker/backend/Dockerfile
    ```

    *for UI Image see [ui/README.md](../ui/README.md)*
