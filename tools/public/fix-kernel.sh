#!/bin/bash -e

__cleanup ()
{
        [[ -f "/etc/apt/sources.list.d/fems-kernel.list" ]] && rm "/etc/apt/sources.list.d/fems-kernel.list"
}

get_hardware () {
	local hardware=$( cat /proc/cpuinfo | grep Hardware );
	local model=$( cat /proc/cpuinfo | grep Model );

	if [[ $hardware == *"BCM"* ]]; then
		if [[ $model == *"Compute Module 4S"* ]]; then
			echo "cm4s";
			return;
		fi;
		if [[ $model == *"Compute Module 3"* ]]; then
			echo "cm3";
			return;
		fi;
		if [[ $model == *"Compute Module 4"* ]]; then
			echo "cm4";
			return;
		fi;
	fi;

	if [[ $hardware == *"Generic AM33"* ]]; then
		echo "beaglebone"
		return;
	fi;

	echo "unknown";
}

fix_config_txt ()
{
if ( grep "\[cm3\]" /boot/config.txt >> /dev/null ); then
       	cat <<EOBASH > /boot/config.txt
# For more options and information see http://rpf.io/configtxt
# Disable unused display settings
disable_overscan=1
camera_auto_detect=0
display_auto_detect=0
disable_splash=1

[pi4]
arm_boost=1

[all]

[cm4]
otg_mode=1

[all]

[cm4s]
otg_mode=1
dtoverlay=pi3-miniuart-bt
dtoverlay=rtc_cm3_tb_v11,mcp7941x

[all]

[pi3]
dtoverlay=dwc2,dr_mode=host
dtoverlay=pi3-miniuart-bt
dtoverlay=rtc_cm3_tb_v11,mcp7941x
# Do not turbo GPU
disable_auto_turbo=1

[all]
dtparam=i2c0=on
i2c0_baudrate=400000
dtoverlay=i2c0,pins_44_45=1
dtparam=i2c1=on
i2c1_baudrate=400000

dtoverlay=disable-wifi

# enable AMA0 RS485
gpio=17=dl"
EOBASH
fi
}

hw=$(get_hardware);
if ! [ $hw == *"cm"* ]; then
	echo -e "\033[0;31mFix not available for Hardware \"$hw\"\033[0m";
	exit 1;
fi;
if ( ! dpkg -s fems-techbase-kernel >/dev/null 2>&1 ); then
	echo -e "\033[0;31mNEUER KERNEL NICHT INSTALLIERT!!!\033[0m";
	exit 1;
fi;

trap __cleanup EXIT;
echo "deb [signed-by=/usr/share/keyrings/fenecon.gpg] http://fenecon.de/debian kernel main" > /etc/apt/sources.list.d/fems-kernel.list

sudo apt update;
sudo apt install fems-techbase-sys-mods;
sudo udevadm control --reload-rules && udevadm trigger;
sudo comctrl -b;

if ! ( grep -q i2c-dev "/etc/modules" ); then
        echo "i2c-dev" >> "/etc/modules";
fi;

if ! ( grep -q "gpio=17=dl" "/boot/config.txt" ); then
        echo "gpio=17=dl" >> "/boot/config.txt";
fi;

#sed -i 's|dtparam=i2c_arm=on||g' /boot/config.txt
fix_config_txt;

systemctl enable fems-pagekite;
systemctl enable comctrl;

if [[ $( du -s /boot/ | awk '{print $1}' ) < 1000 ]]; then
        echo -e "\033[0;31m#############################################\033[0m";
        echo -e "\033[0;31mBOOT ORDNER IST LEER; NICHT NEU STARTEN!!!!!!\033[0m";
        echo -e "\033[0;31m#############################################\033[0m";
	exit 1;
fi

sudo apt-mark manual fems-techbase-kernel fems-techbase-sys-mods;
sudo apt-mark hold fems-techbase-kernel;
