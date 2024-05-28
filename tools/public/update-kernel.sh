#!/bin/bash

hardware=$( cat /proc/cpuinfo | grep Hardware );
model=$( cat /proc/cpuinfo | grep Model );

set -e

check_config () {
	if ! ( grep -q i2c-dev "/etc/modules" ); then
                echo "i2c-dev" >> "/etc/modules";
        fi;

        if ! ( grep -q "gpio=17=dl" "/boot/config.txt" ); then
                echo "gpio=17=dl" >> "/boot/config.txt"
        fi;

        sed -i 's|dtparam=i2c_arm=on|#dtparam=i2c_arm=on|g' /boot/config.txt
}

check_packages () {
	sudo apt install fems-techbase-kernel fems-techbase-sys-mods;

	sudo apt-mark manual fems-techbase-kernel fems-techbase-sys-mods;
        sudo apt-mark hold fems-techbase-kernel;

        systemctl enable comctrl;
        systemctl enable fems-pagekite;

	udevadm control --reload-rules && udevadm trigger;
        comctrl -b;
}

check () {
	check_packages;

	if [[ $( du -s /boot/ | awk '{print $1}' ) < 1000 ]]; then
		echo -e "\033[0;31m#############################################\033[0m";
		echo -e "\033[0;31mBOOT ORDNER IST LEER; NICHT NEU STARTEN!!!!!!\033[0m";
		echo -e "\033[0;31m#############################################\033[0m";
		exit 1;
	fi

	check_config;
}

find_hardware () {
	if [[ $hardware == *"BCM"* ]]; then
        if ( dpkg -s fems-techbase-kernel >/dev/null 2>&1 ); then
                echo -e "\033[0;31mFIX BEREITS ANGEWENDET\033[0m";
                exit 0;
        fi;
        if [[ $model == *"Compute Module 4S"* ]]; then
                echo -e "\033[0;31mFIX CM4S NICHT VERFÜGBAR\033[0m";
                exit 1;
        fi;
        if [[ $model == *"Compute Module 3"* ]]; then
                if ( ! dpkg -s fems-techbase-cm3 >/dev/null 2>&1 ); then
                        echo -e "\033[0;31mALTES CM3 SYSTEM; UPDATE RISKANT!\033[0m";
                        exit 0;
                fi;
                return;
        fi;
        if [[ $model == *"Compute Module 4"* ]]; then
                if ( ! dpkg -s fems-techbase-cm4 >/dev/null 2>&1 ); then
                        echo -e "\033[0;31mALTES CM4 SYSTEM; UPDATE RISKANT!\033[0m";
                        exit 0;
                fi;
                return;
        fi;
	fi;

	if [[ $hardware == *"Generic AM33"* ]]; then
        	echo -e "\033[0;31mFIX FÜR BEAGLEBONE NICHT VERFÜGBAR\033[0m";
        	exit 0;
	fi;

	echo -e "\033[0;31mUnbekannte Hardware\033[0m";
	exit 0;
}

upgrade () {
	echo "deb [signed-by=/usr/share/keyrings/fenecon.gpg] http://fenecon.de/debian kernel main" > /etc/apt/sources.list.d/fems-kernel.list
	sudo apt update;
	if ( dpkg -s fems-techbase-cm3 >/dev/null 2>&1 ); then
		sudo apt install fems-techbase-cm3 -y;
	fi;
	if ( dpkg -s fems-techbase-cm4 >/dev/null 2>&1 ); then
		sudo apt install fems-techbase-cm4 -y;
	fi;
        check;
}

__cleanup ()
{
	[[ -f "/tmp/kernel-update.lock" ]] && rm "/tmp/kernel-update.lock"
	if [[ $? -ne 0 ]]; then
		echo -e "\033[0;31m#####################################\033[0m";
		echo -e "\033[0;31mKERNEL UPDATE NICHT ERFOLGREICH!!!!!!\033[0m";
		echo -e "\033[0;31m#####################################\033[0m";
	fi;
	[[ -f "/etc/apt/sources.list.d/fems-kernel.list" ]] && rm "/etc/apt/sources.list.d/fems-kernel.list"
}

########## MAIN #############
if [ -f /tmp/kernel-update.lock ]; then "echo 'update already running!!!'"; exit 0; fi;

echo -e "\033[1mSTART KERNEL UPDATE\033[0m";

trap __cleanup EXIT;
touch /tmp/kernel-update.lock;

find_hardware;

upgrade;

check;
