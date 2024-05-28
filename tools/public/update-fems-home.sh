#!/bin/bash

set -e

if [ -d "/home/imodcloud" ]; then
	echo "#"
	echo "# Do not apt update Techbase"
	echo "#"
else
	echo "#"
	echo "# Setup dependencies"
	echo "#"
	rm -f /etc/apt/trusted.gpg.d/*.lock
	apt-key adv --recv-key --keyserver keyserver.ubuntu.com 8B48AD6246925553
	apt-key adv --recv-key --keyserver keyserver.ubuntu.com 7638D0442B90D010
	apt --fix-broken install --assume-yes
	apt update
	apt dist-upgrade --assume-yes
fi

if [ "$(which at)" = "" ]; then
	echo "# Install 'at'"	
	apt install at
fi
if [ "$(which ncdu)" = "" ]; then
	echo "# Install 'ncdu'"	
	apt install ncdu
fi
export PACKAGE="openems-core"
export PACKAGE_UNDERSCORES="${PACKAGE//-/_}"
export CURRENT_VERSION="$(dpkg-query --showformat='${Version}' --show $PACKAGE)"
export LATEST_VERSION="$(wget -qO- http://fenecon.de/debian-test/${PACKAGE_UNDERSCORES}-latest.version)"
if [ "$CURRENT_VERSION" = "$LATEST_VERSION" ]; then
	echo "#"
	echo "# $PACKAGE: latest version [$LATEST_VERSION] is already installed"
	echo "#"
else
	echo "#"
	echo "# $PACKAGE: Updating from version [$CURRENT_VERSION] to [$LATEST_VERSION]"
	echo "#"
	wget http://fenecon.de/debian-test/${PACKAGE_UNDERSCORES}-latest.deb -q --show-progress -O /tmp/${PACKAGE_UNDERSCORES}-latest.deb && 
		dpkg -i /tmp/${PACKAGE_UNDERSCORES}-latest.deb && 
		rm -f /tmp/${PACKAGE_UNDERSCORES}-latest.deb
fi

export PACKAGE="openems-core-fems"
export PACKAGE_UNDERSCORES="${PACKAGE//-/_}"
export CURRENT_VERSION="$(dpkg-query --showformat='${Version}' --show $PACKAGE)"
export LATEST_VERSION="$(wget -qO- http://fenecon.de/debian-test/${PACKAGE_UNDERSCORES}-latest.version)"
if [ "$CURRENT_VERSION" = "$LATEST_VERSION" ]; then
	echo "#"
	echo "# $PACKAGE: latest version [$LATEST_VERSION] is already installed"
	echo "#"
else
	echo "#"
	echo "# $PACKAGE: Updating from version [$CURRENT_VERSION] to [$LATEST_VERSION]"
	echo "#"
	wget http://fenecon.de/debian-test/${PACKAGE_UNDERSCORES}-latest.deb -q --show-progress -O /tmp/${PACKAGE_UNDERSCORES}-latest.deb && 
		dpkg -i /tmp/${PACKAGE_UNDERSCORES}-latest.deb && 
		rm -f /tmp/${PACKAGE_UNDERSCORES}-latest.deb
fi

echo "#"
echo "# Check enx.network"
if [ -e /etc/systemd/network/enx.network ]; then
    echo "# -> exists"
else
    echo "#"
    echo "# -> enx.network DOES NOT exist!!!"
    echo "#"
fi

echo "#"
echo "# Update to openems.jar for FENECON Home"
echo "#"
wget http://fenecon.de/debian-test/home/openems.jar -O /usr/lib/openems/openems.jar

echo "#"
echo "# Restart OpenEMS"
echo "#"
systemctl restart openems --no-block

echo "#"
echo "# Finished"
echo "#"
