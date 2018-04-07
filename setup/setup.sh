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

	#selection process	
	CHOICE=$(whiptail --menu "Wähle aus:" 20 45 12 \
			"FENECON Mini" "" \
			"FENECON DESS" "" \
			"FENECON Pro" "" \
			"FENECON Pro Cluster" ""\
			"FENECON Pro AC-Insel" "" \
			"FENECON Pro Heizstab" "" \
			"FENECON Pro Waermepumpe" "" \
			"FENECON Commercial AC" "" \
			"FENECON Commercial DC" "" \
			"FENECON Commercial Hybrid" "" 3>&1 1>&2 2>&3)

	if [ $? = 0 ]; then

			case $CHOICE in
					"FENECON Mini")             	echo "Es wurde Mini gewählt";;
					"FENECON DESS")                 echo "Es wurde Pro Hybrid gewählt";;
					"FENECON Pro")             		echo "Es wurde Pro 9-12 gewählt";;
					"FENECON Pro Cluster")			echo "Es wurde Pro 18-24 gewählt";;
					"FENECON Pro AC-Insel")         echo "Es wurde AC-Insel gewählt";;
					"FENECON Pro Heizstab")			echo "Es wurde Heizstab gewählt";;
					"FENECON Pro Waermepumpe")      echo "Es wurde Wärmepumpe gewählt";;
					"FENECON Commercial AC")        echo "Es wurde Commercial AC gewählt";;
					"FENECON Commercial DC")        echo "Es wurde Commercial DC gewählt";;
					"FENECON Commercial Hybrid")    echo "Es wurde Comemrcial Hybrid gewählt";;

			esac

	else
			echo "Nichts ausgewählt"
			exit
	fi
	
	echo "#save variable to file"
	touch /opt/choice && echo $CHOICE > /opt/choice

	echo "# Copy templates to target"
	cp -r /boot/uboot/fems-setup/templates /opt/fems-setup/

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

	echo "# Mark first stage as finished"
	touch /mnt$FINISHED_1
	/bin/umount /mnt
	
	echo "# Blink all LEDs"
	echo timer | tee /sys/class/leds/beaglebone:green:usr?/trigger >/dev/null

	read -p "Press [Enter] key to shutdown"

	echo "# Shutdown system"
	shutdown -h now

else
	echo "#"
	echo "# Starting second stage of FEMS setup"
	echo "#"

	cd /opt/fems-setup
	FEMS=$(head -n 2 devices | tail -n 1 | cut -d ";" -f 1)
	PASSWORD=$(head -n 2 devices | tail -n 1 | cut -d ";" -f 2)
	

	echo "# Set password for user fems"
	echo "fems:${PASSWORD}" | /usr/sbin/chpasswd	

	echo "# Set name to ${FEMS}"
	echo $FEMS > /etc/hostname
	/bin/sed "s/\(127.0.1.1.*\)fems.*/\1$FEMS/" /etc/hosts --in-place
	echo $FEMS > /etc/mailname
	rm -fv /etc/ssh/ssh_host_*
	dpkg-reconfigure openssh-server
	rm -f /etc/udev/rules.d/70-persistent-net.rules

	echo "# Mark second stage as finished"
	touch $FINISHED_2

	echo "#"
	echo "# Starting third stage of FEMS setup"
	echo "#"
	
	cd /opt/fems-setup
	PACKAGES=$(head -n 2 devices | tail -n 1 | cut -d ";" -f 4)
	
	echo "# Add FENECON debian repository"
	wget -O - http://fenecon.de/debian/fems.gpg.key | apt-key add -
	wget http://fenecon.de/debian/fems.list -O /etc/apt/sources.list.d/fems.list
	echo "# Add Debian Backports repository"
	echo "deb http://ftp.de.debian.org/debian/ jessie-backports main contrib non-free" > /etc/apt/sources.list.d/jessie-backports.list
	echo "# Refresh apt cache"
	/usr/bin/aptitude update
	echo "# Install openjdk 8"
	/usr/bin/apt install -t jessie-backports openjdk-8-jre-headless --assume-yes

	CHOICE=$(cat /opt/choice)

	if [ "$CHOICE" == "FENECON DESS" ]; then

		aptitude install fems-dess fems-fenecononlinemonitoring --assume-yes

	else
		aptitude install openems openems-fems openems-ui influxdb grafana --assume-yes
		
		# copy config and set name + apikey
		CHOICE=$(cat /opt/choice)	
		mkdir -p /opt/config.d

		echo "# Write Apikey into /etc/fems"
		APIKEY=$(head -n 2 /opt/fems-setup/devices | tail -n 1 | cut -d ";" -f 3)
		echo "apikey=${APIKEY}" > /etc/fems

		cp "/opt/fems-setup/templates/$CHOICE.json" "/opt/config.d/$CHOICE.json"
		NAME=$(head -n 2 /opt/fems-setup/devices | tail -n 1 | cut -d ";" -f 1)

		sed "s/\"###FEMS_ID###\"/${NAME:4}/" --in-place "/opt/config.d/$CHOICE.json" 
		
		APIKEY=$(head -n 2 /opt/fems-setup/devices | tail -n 1 | cut -d ";" -f 3) 	
		sed "s/###APIKEY###/$APIKEY/" --in-place "/opt/config.d/$CHOICE.json"
	
		CHOICE=$(cat /opt/choice)	
		mv "/opt/config.d/$CHOICE.json" "/etc/openems.d/config.json"
		unlink /opt/choice

	fi

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
	
	#echo "# Finialize setup with fems-autoupdate fems"
	#/usr/bin/fems-autoupdate fems
	
	echo "#"
	echo "# Finished setup"
	echo "#"
	cd /opt/scripts/tools/
	git pull
	./update_kernel.sh --lts-4_14
	cd /home/fems
	fems-autoupdate fems -y || true

	echo "# Blink all LEDs"
	echo timer | tee /sys/class/leds/beaglebone:green:usr?/trigger >/dev/null
	
	echo "# Rebooting system"
	reboot
fi
