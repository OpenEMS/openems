# Deploy local OpenEMS Edge changes to Docker in an LXC

This guide deploys the current Windows working tree to an existing OpenEMS Edge container whose Docker Engine runs inside an LXC. It uses an SSH Docker context so the image is built directly on the LXC; Docker Desktop is not required, but the Docker CLI and SSH client must be available on Windows.

The commands use the default Edge container names from this directory's Compose file: service `openems-edge`, container `openems_edge`. If your installation uses different names or a different Compose file, substitute the values found in the inspection step below.

## 1. Check access and identify the existing deployment

Open PowerShell in the root of the local OpenEMS checkout. Set the SSH destination to the LXC account and host:

```powershell
$Lxc = "USER@LXC_HOST"
```

Create a Docker context once. If `openems-lxc` already exists in `docker context ls`, skip the create command.

```powershell
docker context create openems-lxc --docker "host=ssh://$Lxc"
docker --context openems-lxc info --format '{{.OSType}}/{{.Architecture}}'
docker --context openems-lxc ps --filter name=openems_edge --format 'table {{.Names}}\t{{.Image}}\t{{.Status}}'
```

The Docker info command must connect to the LXC and report a Linux platform. SSH access alone does not guarantee permission to use Docker; if access is denied, arrange Docker daemon permissions for this SSH account before continuing.

Inspect the running Edge container and note its Compose file, working directory, service name and image tag:

```powershell
docker --context openems-lxc inspect openems_edge --format 'image={{.Config.Image}} project={{index .Config.Labels "com.docker.compose.project"}} service={{index .Config.Labels "com.docker.compose.service"}} working_dir={{index .Config.Labels "com.docker.compose.project.working_dir"}} compose_files={{index .Config.Labels "com.docker.compose.project.config_files"}}'
```

If the Compose labels are empty, the container was probably started with `docker run` or another tool. Do not remove or recreate it using the Compose commands below until you know its original ports, mounts and environment settings.

## 2. Build the image from the local working tree

The Docker context is the current checkout (`.`), so it includes the uncommitted Victron changes and the new `BatterySource.java` file. The Edge Dockerfile runs `buildEdge` and packages the Edge application.

Review untracked and local files before sending the build context to the LXC. There is no root `.dockerignore`, so the context can include the whole checkout, including local files that are not part of OpenEMS.

Optionally run the focused Victron tests locally before building:

```powershell
.\gradlew.bat :io.openems.edge.victron:test
```

```powershell
$env:DOCKER_BUILDKIT = "1"
docker --context openems-lxc build --no-cache `
  --tag openems/edge:local-victron-aggregator `
  --file tools/docker/edge/Dockerfile .
```

The build may take several minutes and requires sufficient disk space on the LXC. The `local-victron-aggregator` tag is intentionally separate from `latest`, which preserves the old image for rollback.

Confirm that the image now exists on the LXC:

```powershell
docker --context openems-lxc image inspect openems/edge:local-victron-aggregator --format '{{.Os}}/{{.Architecture}}'
```

If the build fails on the Dockerfile's `RUN --mount` instruction, the LXC Docker Engine needs BuildKit support. Do not remove the existing container or image while resolving this.

## 3. Back up the Compose file and persistent configuration

Connect to the LXC:

```powershell
ssh $Lxc
```

On the LXC, use the `working_dir` and `compose_files` values from step 1. Change to the original Compose working directory; use the same project and Compose file as the running container so Docker keeps using its existing named volumes.

```sh
cd /PATH/TO/COMPOSE/WORKING/DIRECTORY
cp docker-compose.yml docker-compose.yml.before-local-victron
docker cp openems_edge:/var/opt/openems/config "$HOME/openems-config-backup-$(date +%Y%m%d-%H%M%S)"
```

If your Compose file has another name, use that name in the backup command. The config copy is an additional backup; the Compose file's existing config and data volume declarations must remain unchanged.

## 4. Point only the Edge service at the local image and recreate it

Edit the existing Compose file on the LXC. Change only the Edge service's image:

```yaml
image: openems/edge:local-victron-aggregator
```

Keep its service name, ports, volumes, restart policy and all UI settings unchanged. For the repository's standard Compose file, the service name is `openems-edge` (the container name is `openems_edge`).

From the same Compose working directory, recreate only Edge:

```sh
docker compose -f /PATH/TO/docker-compose.yml up -d --no-deps --force-recreate --pull never openems-edge
```

Do not run `docker compose down -v`: that can delete persistent OpenEMS config and data volumes. Do not run `docker compose pull` for the local test tag.

## 5. Verify the container and configure the battery

Check that the container uses the local image and inspect its startup log:

```sh
docker inspect openems_edge --format '{{.Config.Image}}'
docker compose -f /PATH/TO/docker-compose.yml ps
docker logs --tail 100 openems_edge
```

In the OpenEMS UI, configure one `Victron.Battery` component for the aggregate service:

* Battery source: `DBUS_AGGREGATOR`
* Modbus Unit-ID: `99`
* Modbus ID and ESS ID: the IDs used by your existing components
* Nominal battery voltage: the battery bank's rated nominal voltage (decimal values are supported; do not use the charge voltage)
* Minimum discharge voltage: the value approved for your batteries by their manufacturer/BMS (decimal values are rounded up to the next whole volt)

Do not copy a sample voltage from another setup. Before enabling managed battery operation, confirm the GX exposes the aggregate at DeviceInstance 99 and verify that voltage, current, SoC, capacity, charge/discharge limits and the configured minimum discharge voltage are sensible.

## Roll back

In the Compose file, restore the previous image tag recorded in step 1 (for the standard setup, `openems/edge:latest`) and run the same `docker compose up` command again. Keep the local image and the config backup until the system has been verified.

## Alternative: build on Windows and transfer the image

If Docker Desktop or another local Linux Docker Engine is already available, build locally for the LXC's architecture instead of sending the source tree to the LXC. For an `amd64` LXC:

```powershell
docker buildx build --platform linux/amd64 --load `
  --tag openems/edge:local-victron-aggregator `
  --file tools/docker/edge/Dockerfile .
docker save --output .\openems-edge-local.tar openems/edge:local-victron-aggregator
scp .\openems-edge-local.tar "${Lxc}:/tmp/openems-edge-local.tar"
ssh $Lxc "docker load -i /tmp/openems-edge-local.tar"
```

Use the platform reported in step 1; Docker reports `x86_64` as `amd64` and `aarch64` as `arm64` in the `--platform` value. After loading, continue from step 3. Remove the temporary archive from Windows and `/tmp` on the LXC after successful deployment.
