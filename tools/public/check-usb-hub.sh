#!/bin/bash

set -e

echo "#"
echo "# Prüfe ob das Update alle relevanten Dateien angelegt hat"
echo "#"

echo "#"
echo "# Check enx.network"
if [ -e /etc/systemd/network/enx.network ]; then
    echo "# -> exists"
	
	echo "#"
	echo "# Check usb rules"
	if [ -e /etc/udev/rules.d/98-fems-default.rules ]; then
    echo "# -> exists"
	exit
else
    echo "#"
    echo "# -> usb rules DOES NOT exist!!!"
    echo "#"
fi
else
    echo "#"
    echo "# -> enx.network DOES NOT exist!!!"
    echo "#"
fi

echo "#"
echo "# Stop OpenEMS"
echo "#"
systemctl stop openems

echo "#"
echo "# Setup dependencies"
echo "#"
rm -f /etc/apt/trusted.gpg.d/*.lock
apt-key adv --recv-key --keyserver keyserver.ubuntu.com 8B48AD6246925553
apt-key adv --recv-key --keyserver keyserver.ubuntu.com 7638D0442B90D010
apt --fix-broken install --assume-yes
apt update
apt dist-upgrade --assume-yes
apt install ncdu at --assume-yes

echo "#"
echo "# Update openems-core"
echo "#"
wget http://fenecon.de/debian-test/openems_core-latest.deb -O /tmp/openems_core-latest.deb 
dpkg -i /tmp/openems_core-latest.deb
rm -f /tmp/openems_core-latest.deb

echo "#"
echo "# Update openems-core-fems"
echo "#"
wget http://fenecon.de/debian-test/openems_core_fems-latest.deb -O /tmp/openems_core_fems-latest.deb
dpkg -i /tmp/openems_core_fems-latest.deb
rm -f /tmp/openems_core_fems-latest.deb

echo "#"
echo "# Restart OpenEMS"
echo "#"
systemctl restart openems --no-block

echo "#"
echo "# Finished"
echo "#"
