#!/bin/bash

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

upgrade () {
	check_config;

	echo "deb [signed-by=/usr/share/keyrings/fenecon.gpg] http://fenecon.de/debian kernel main" > /etc/apt/sources.list.d/fems-kernel.list

	sudo apt update
	sudo apt reinstall -y fems-techbase-sys-mods;

	sudo apt-mark manual fems-techbase-sys-mods;

	systemctl enable comctrl;
	comctrl -b;
}

__cleanup ()
{
	[[ -f "/tmp/kernel-update.lock" ]] && rm "/tmp/kernel-update.lock"
	if [[ $? -ne 0 ]]; then
		echo -e "\033[0;31m#####################################\033[0m";
		echo -e "\033[0;31mSYS-MODS UPDATE NICHT ERFOLGREICH!!!!!!\033[0m";
		echo -e "\033[0;31m#####################################\033[0m";
	fi;
	[[ -f "/etc/apt/sources.list.d/fems-kernel.list" ]] && rm "/etc/apt/sources.list.d/fems-kernel.list"
}

########## MAIN #############
if [ -f /tmp/kernel-update.lock ]; then "echo 'update already running!!!'"; exit 0; fi;

echo -e "\033[1mSTART SYS-MODS UPDATE\033[0m";

trap __cleanup EXIT;
touch /tmp/kernel-update.lock;

upgrade;
