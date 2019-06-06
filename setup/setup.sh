#!/bin/bash
#
# Script to automatically setup a FEMS.
# Setup parameters are taken from the first line of a devices file in the format:
# <fems-id>;<password>;<apikey>;<packages>
#
set -e

# Define environment variables
BASE_PATH="/opt/fems-setup"
FILE_CHOICE="${BASE_PATH}/choice"
FILE_FINISHED_1="${BASE_PATH}/finished_1"
FILE_FINISHED_2="${BASE_PATH}/finished_2"
FILE_DEVICES="${BASE_PATH}/devices"

# First stage: Copy filesystem to internal eMMC
if [ ! -e $FILE_FINISHED_1 ]; then
	echo "#"
	echo "# Starting first stage of FEMS setup"
	echo "#"
	
	if [ "$(cat $FILE_DEVICES | wc -l)" == "1" ]; then
		echo "# No devices left in configuration file"
		echo "#"
		echo "# Aborting"
		exit 1
	fi

	#selection process	
	CHOICE=$(whiptail --menu "Select:" 20 45 12 \
			"FENECON Pro Hybrid 10-Serie" "" \
			"FENECON Mini" "" \
			"FENECON DESS" "" \
			"FENECON Pro" "" \
			"FENECON Pro mit Gavazzi" ""\
			"FENECON Pro Cluster" ""\
			"FENECON Pro AC-Insel" "" \
			"FENECON Pro Heizstab" "" \
			"FENECON Pro Waermepumpe" "" \
			"FENECON Commercial AC" "" \
			"FENECON Commercial DC" "" \
			"FENECON Commercial Hybrid" "" 3>&1 1>&2 2>&3)

	if [ $? = 0 ]; then

			case $CHOICE in
					"FENECON Pro Hybrid 10-Serie")  echo "Es wurde FENECON Pro Hybrid 10-Serie gewählt";;
					"FENECON Mini")             	echo "Es wurde Mini gewählt";;
					"FENECON DESS")                 echo "Es wurde DESS (Pro Hybrid) gewählt";;
					"FENECON Pro")             		echo "Es wurde Pro 9-12 gewählt";;
					"FENECON Pro mit Gavazzi")      echo "Es wurde Pro-CarloGavazzi gewählt";;
					"FENECON Pro Cluster")			echo "Es wurde Pro 18-24 gewählt";;
					"FENECON Pro AC-Insel")         echo "Es wurde AC-Insel gewählt";;
					"FENECON Pro Heizstab")			echo "Es wurde Heizstab gewählt";;
					"FENECON Pro Waermepumpe")      echo "Es wurde Wärmepumpe gewählt";;
					"FENECON Commercial AC")        echo "Es wurde Commercial AC gewählt";;
					"FENECON Commercial DC")        echo "Es wurde Commercial DC gewählt";;
					"FENECON Commercial Hybrid")    echo "Es wurde Commercial Hybrid gewählt";;
			esac

	else
			echo "Nichts ausgewählt"
			exit 1
	fi
	
	echo "# Save choice to file $FILE_CHOICE"
	echo $CHOICE > $FILE_CHOICE

	echo "# Copy filesystem to internal eMMC"
	/opt/scripts/tools/eMMC/bbb-eMMC-flasher-eewiki-ext4.sh
	
	echo "# Mount target filesystem"
	/bin/mount /dev/mmcblk1p1 /mnt

	echo "# Clear devices list on target"
	/usr/bin/head -n 2 "${FILE_DEVICES}" > "/mnt${FILE_DEVICES}"

	echo "# Remove fems from local devices list"
	/bin/sed --expression='2d' "${FILE_DEVICES}" --in-place
	
	echo "# Mark first stage as finished"
	touch "/mnt${FILE_FINISHED_1}"
	
	echo "# Unmount target filesystem"
	/bin/umount /mnt
	
	echo "# Blink all LEDs"
	echo timer | tee /sys/class/leds/beaglebone:green:usr?/trigger >/dev/null

	read -p "Press [Enter] key to shutdown"

	echo "# Shutdown system"
	shutdown -h now

elif [ ! -e $FILE_FINISHED_2 ]; then

	echo "#"
	echo "# Starting second stage of FEMS setup"
	echo "#"

	FEMS=$(head -n 2 $FILE_DEVICES | tail -n 1 | cut -d ";" -f 1)
	PASSWORD=$(head -n 2 $FILE_DEVICES | tail -n 1 | cut -d ";" -f 2)
	APIKEY=$(head -n 2 $FILE_DEVICES | tail -n 1 | cut -d ";" -f 3)
	CHOICE=$(cat $FILE_CHOICE)
	
	echo "# Write Apikey into /etc/fems"
	echo "apikey=${APIKEY}" > /etc/fems
	
	echo "# Set password for user fems"
	echo "fems:${PASSWORD}" | /usr/sbin/chpasswd

	echo "# Set name to ${FEMS}"
	echo $FEMS > /etc/hostname
	/bin/sed "s/fems/$FEMS/g" /etc/hosts --in-place
	
	echo "# Update SSH keys"
	rm -fv /etc/ssh/ssh_host_*
	/usr/sbin/dpkg-reconfigure openssh-server

	echo "# Refresh apt cache"
	/usr/bin/aptitude update

	echo "# Apply choice $CHOICE"
	if [ "$CHOICE" == "FENECON DESS" ]; then

		echo "deb http://deb.debian.org/debian jessie main contrib non-free" > /etc/apt/sources.list.d/jessie.list
		wget http://ftp.de.debian.org/debian/pool/main/t/tzdata/tzdata_2018e-0+deb8u1_all.deb
		dpkg -i tzdata_2018e-0+deb8u1_all.deb
		apt-get remove openjdk-8-jre-headless
		apt-get -o Dpkg::Options::="--force-overwrite" install fems
		aptitude install fems-dess fems-fenecononlinemonitoring influxdb grafana --assume-yes || true
		systemctl start fems-openhab
		systemctl enable fems-openhab

	elif [ "$CHOICE" == "FENECON Pro Hybrid 10-Serie" ]; then
		
		aptitude install openems-core openems-core-fems --assume-yes
		
	else
		
		aptitude install openems openems-fems openems-ui influxdb grafana --assume-yes
		
		FILE_CONFIG="/etc/openems.d/config.json"
		echo "# Create $FILE_CONFIG"
		cp "${BASE_PATH}/templates/${CHOICE}.json" "$FILE_CONFIG"
		sed "s/\"###FEMS_ID###\"/${FEMS:4}/" "$FILE_CONFIG" --in-place
		sed "s/###APIKEY###/$APIKEY/" "$FILE_CONFIG" --in-place
	
	fi

	echo "# fems-autoupdate fems"
	/usr/bin/fems-autoupdate fems || true

	echo "# Clear files"
	rm "$FILE_DEVICES"
	rm "$FILE_CHOICE"
	
	echo "#"
	echo "# Finished setup"
	echo "#"

	echo "# Blink all LEDs"
	echo timer | tee /sys/class/leds/beaglebone:green:usr?/trigger >/dev/null
	
	echo "# Mark second stage as finished"
	touch $FILE_FINISHED_2
	
	echo "# Rebooting system"
	reboot
fi
