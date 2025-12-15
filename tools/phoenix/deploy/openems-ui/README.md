# HOWTO deploy OpenEMS-UI on Device

This HOWTO contains advice to install "OpenEMS-UI" on devices and info for trouble shooting.

## Installation

Steps to take:

1. Place sub-folder structure located under `tools/phoenix/deploy/openems-ui` on device.
1. Add a link to `/etc/nginx/sites-available/openems-ui.conf` in folder `/etc/nginx/sites-enabled` NOTE: link name needs to end witn `.conf`!
1. The module "OpenEMS-UI" located under `ui/` needs to be built anyway, because there is no package. Do this following these steps:
    1. [Download Node.js version 22.x.y](https://nodejs.org/en/download) for platform of your build machine.
    1. Install Node.js version 22.x.y. on your build machine.
    1. Verify that Node.js has been installed successfully with `node --version`.
    1. Checkout `develop` branch or latest release tag from [OpenEMS repository](https://github.com/OpenEMS/openems).
    1. Change to folder `ui/`.
    1. Run `npm install` to resolve dependencies and install Node.js modules required by "OpenEMS-UI".
    1. Run `./node_modules/.bin/ng build -c "openems,openems-edge-docker" --base-href /openems-ui/` to build the app properly.
1. Upload content of `ui/target` folder to path `/opt/plcnext/apps/openems-ui/html`. NOTE: Not the `target` folder itself, the files within the `target` folder need to be uploaded to the specified directory on the device.
1. Restart NGINX on device.
1. Check functionality of NGINX, URL (https://192.168.1.10) should be still available.
1. Enable "Controller Api Websocket" using [Apache Felix Console](http://192.168.1.10:8080/system/console/configMgr) to listen to port `8075`.
1. Open "WBM" and enable port `8075` to be accessed from outside of the device, because "OpenEMS-UI" is running as "fat web client" in users browser.
1. Point your browser to ["OpenEMS-UI" on device](https://192.168.1.10/openems-ui)
1. Use default passwd "admin" to login to "OpenEMS-UI" module as guest user.
1. Change password!

## Troubleshooting

* Installing the NGINX config for "OpenEMS-UI" may cause that the NGINX on device ist not responding anymore. In this case it's necessary to check the logs what had happened, do a device reset, re-update the firmware, adjust the NGINX config and try again.
* ...