#!/bin/bash
set -e

APIKEY=$(grep 'apikey=' /etc/fems | cut -d'=' -f2)
FEMS=$(/bin/hostname)
FEMS_ID=${FEMS:4}
FILE="/etc/openems.d/config.json"

echo "============================================="
echo "Setup ${FEMS} with apikey ${APIKEY}"
echo "============================================="

echo "- finish pending debian activities"
dpkg --configure -a
apt-get install -f

echo "- add FENECON apt repository"
echo "deb http://fenecon.de/debian jessie main" > /etc/apt/sources.list.d/fems.list

if [ $(grep "^[^#;]" /etc/apt/sources.list | grep "jessie-backports" | wc -l) -eq 0 ]; then
	echo "- add Debian bacports apt repository"
	echo "deb http://ftp.de.debian.org/debian/ jessie-backports main contrib non-free" >> /etc/apt/sources.list
fi

echo "- update apt cache"
apt-get update

echo "- Create config file /etc/openems.d/config.json"
mkdir -p /etc/openems.d

cat >$FILE <<END
{
        "things": [
                {
                        "class": "io.openems.impl.protocol.modbus.ModbusRtu",
                        "serialinterface": "/dev/ttyUSB0",
                        "baudrate": 9600,
                        "databits": 8,
                        "parity": "none",
                        "stopbits": 1,
                        "devices": [
                                {
                                        "class": "io.openems.impl.device.minireadonly.FeneconMini",
                                        "modbusUnitId": 4,
                                        "ess": {
                                                "id": "ess0",
                                                "minSoc": 15
                                        },
                                        "gridMeter": {
                                                "id": "meter0"
                                        },
                                        "productionMeter": {
                                                "id": "meter1"
                                        },
                                        "consumptionMeter": {
                                                "id": "meter2"
                                        }
                                }
                        ]
                }
        ],
        "scheduler": {
                "class": "io.openems.impl.scheduler.SimpleScheduler",
                "cycleTime": 10000,
                "controllers": [
                        {
                                "priority": 150,
                                "class": "io.openems.impl.controller.debuglog.DebugLogController",
                                "esss": [
                                        "ess0"
                                ],
                                "meters": [
                                        "meter0",
                                        "meter1",
                                        "meter2"
                                ]
                        }
                ]
        },
        "persistence": [
                {
                        "class": "io.openems.impl.persistence.influxdb.InfluxdbPersistence",
                        "ip": "127.0.0.1",
                        "fems": "###FEMS###"
                },
                {
                        "class": "io.openems.impl.persistence.fenecon.FeneconPersistence",
                        "apikey": "###APIKEY###"
                }
        ]
}
END

sed --in-place "s/\"###FEMS###\"/$FEMS_ID/" $FILE
sed --in-place "s/###APIKEY###/$APIKEY/" $FILE

echo "- Install JRE 8 from backports"
yes | apt install -t jessie-backports openjdk-8-jre-headless

echo "- Install packages"
aptitude install -y openems openems-fems openems-ui influxdb grafana

echo "- Remove old packages"
aptitude remove -y fems-openhab-addon-astro fems-openhab-addon-fems fems-openhab-addon-fenecon fems-openhab-addon-fenecononlinemonitoring fems-openhab-addon-forecastio fems-openhab-runtime fems-orion

echo "- Clear systemd"
systemctl daemon-reload
systemctl stop fems-openhab || true

echo "- autoupdate fems"
fems-autoupdate fems || true

echo "- Finished"

journalctl -lfu openems
