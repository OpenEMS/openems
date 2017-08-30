#!/bin/bash
#
# Script to automatically setup a FEMS.
# Setup parameters are taken from the first line of a devices file in the format:
# <fems-id>;<password>;<apikey>;<packages>
#
set -e

# Define status files
FINISHED_1=/opt/fems-setup/finished_1
FINISHED_2=/opt/fems-setup/finished_2
DEVICES=/opt/fems-setup/devices

# First stage: Copy filesystem to internal eMMC
if [ ! -e $FINISHED_1 ]; then
	echo "#"
	echo "# Starting first stage of FEMS setup"
	echo "#"
	
	if [ "$(cat /boot/uboot/fems-setup/devices | wc -l)" == "1" ]; then
		echo "# No devices left in configuration file"
		echo "#"
		echo "# Aborting"
		exit 1
	fi
	
	echo "# Copy filesystem to internal eMMC"
	cd /opt/scripts/tools/eMMC
	./bbb-eMMC-flasher-eewiki-ext4.sh || true
	
	echo "# Mount target filesystem"
	/bin/mount /dev/mmcblk1p1 /mnt

	echo "# Copy setup script to target"
	mkdir -p /mnt/opt/fems-setup
	cp /boot/uboot/fems-setup/setup.sh /mnt/opt/fems-setup/

	echo "# Copy current fems data to target"
	/usr/bin/head -n 2 /boot/uboot/fems-setup/devices > /mnt/opt/fems-setup/devices
	
	echo "# Remove fems from devices list"
	/bin/sed --expression='2d' /boot/uboot/fems-setup/devices --in-place
	
	echo "# Write Apikey into /etc/fems"
	/usr/bin/head -n 2 devices | tail -n 1 | cut -d ";" -f 3 > /mnt/etc/fems
	
	echo "# Mark first stage as finished"
	touch /mnt$FINISHED_1
	/bin/umount /mnt
	
	echo "# Blink all LEDs"
	echo timer | tee /sys/class/leds/beaglebone:green:usr?/trigger >/dev/null

	read -p "Press [Enter] key to shutdown"

	echo "# Shutdown system"
	/sbin/shutdown -h now

elif [ ! -e $FINISHED_2 ]; then
	echo "#"
	echo "# Starting second stage of FEMS setup"
	echo "#"

	FEMS=$(head -n 2 $DEVICES | tail -n 1 | cut -d ";" -f 1)
	PASSWORD=$(head -n 2 $DEVICES | tail -n 1 | cut -d ";" -f 2)
	
	echo "# Set password for user fems"
	echo "fems:${PASSWORD}" | /usr/sbin/chpasswd	

	echo "# Set name to ${FEMS}"
	echo $FEMS > /etc/hostname
	/bin/sed "s/\(127.0.1.1.*\)fems.*/\1$FEMS/" /etc/hosts --in-place
	echo $FEMS > /etc/mailname
	rm -fv /etc/ssh/ssh_host_*
	dpkg-reconfigure openssh-server
	rm /etc/udev/rules.d/70-persistent-net.rules

	echo "# Blink all LEDs"
	echo timer | tee /sys/class/leds/beaglebone:green:usr?/trigger >/dev/null
	
	echo "# Mark second stage as finished"
	touch $FINISHED_2

	read -p "Press [Enter] key to reboot"
	echo "# Rebooting system"
	reboot

else
	echo "#"
	echo "# Starting third stage of FEMS setup"
	echo "#"
	
	PACKAGES=$(head -n 2 $DEVICES | tail -n 1 | cut -d ";" -f 4)
	
	echo "# Add FENECON debian repository"
	wget -O - http://fenecon.de/debian/fems.gpg.key | apt-key add -
	wget http://fenecon.de/debian/fems.list -O /etc/apt/sources.list.d/fems.list
	echo "# Add Debian Backports repository"
	echo "deb http://ftp.de.debian.org/debian/ jessie-backports main contrib non-free" > /etc/apt/sources.list.d/jessie-backports.list
	echo "# Refresh apt cache"
	/usr/bin/aptitude update
	echo "# Install openjdk 8"
	/usr/bin/apt install -t jessie-backports openjdk-8-jre-headless
	
	# this is not working...
	#echo "# preset apikey for dpkg-configure"
	#. /usr/share/debconf/confmodule
	#db_set fems-openhab-addon-fems/apikey $APIKEY
	
	# TODO remove setup-script from /opt/fems-setup

	if [ "${PACKAGES}" != "" ]; then
		echo "# Install packages: ${PACKAGES}"
		/usr/bin/aptitude install --assume-yes $PACKAGES
	else
		echo "# Install NO packages!"
	fi
	
	echo "# Finialize setup with fems-autoupdate fems"
	/usr/bin/fems-autoupdate fems
	
	echo "# Blink all LEDs"
	echo timer | tee /sys/class/leds/beaglebone:green:usr?/trigger >/dev/null
	
	echo "#"
	echo "# Finished setup"
	echo "#"
	
	read -p "Press [Enter] key to reboot"
	echo "# Rebooting system"
	reboot
fi
