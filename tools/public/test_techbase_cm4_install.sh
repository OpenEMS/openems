#!/bin/bash
# File: install.sh

BCyan='\033[1;36m'        # Cyan


if [ $(hostname) = techbase ]; then

    printf '%s\n' "Starting installation script for techbase cm4 | Starte Installationsscript für techbase cm4 hardware"
else

	if [ $(hostname) = femsxxx ]; then
	
		printf '%s\n' "Starting script for the second time. Feature maybe added in future | Script wird zum zweiten mal ausgeführt. Diese Funktion wird eventuell noch kommen."
		exit
	else
		printf '%s\n' "Starting script on a wrong device | Script funktioniert nicht auf diesem Gerät."
		exit
	fi
fi




# check if the reboot flag file exists. 
# We created this file before rebooting.
if [ ! -f /var/run/resume_after_reboot ]; then
  echo -e "\e ${BCyan} === running script for the first time... === \e[0m"
echo "Read FEMS"
read FEMS
echo "Read APIKEY"
read APIKEY
echo -e "\e ${BCyan} fems=${FEMS}, apikey=${APIKEY} \e[0m"

read -p "Are you sure y/n? " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]
then
    [[ "$0" = "$BASH_SOURCE" ]] && exit 1 || return 1
fi

deluser user
update-rc.d imodcloud_start remove
systemctl stop openvpn
systemctl disable openvpn
apt update --assume-yes
apt remove --purge ntp mosquitto libmosquitto1 mosquitto-clients x11-common bluez gnome-accessibility-themes gnome-icon-theme gnome-menus gnome-themes-extra gnome-themes-extra-data gnome-themes-standard libgnome-menu-3-0 libsoup-gnome2.4-1 desktop-base gpicview gsettings-desktop-schemas gstreamer1.0* gtk-update-icon-cache gtk2-engines iw lx* omxplayer owserver ow-shell owfs-common --assume-yes
apt autoremove --purge --assume-yes
echo -e "\e ${BCyan} === Pakete installieren === \e[0m "
apt install vim ntpdate aptitude ufw screen python-socksipychain python-pymodbus nginx-light at openjdk-8-jre-headless git python-urllib3 lsof --assume-yes
echo -e "\e ${BCyan} === FEMS konfigurieren === \e[0m"
useradd --comment FEMS --groups sudo,root --shell /bin/bash --create-home fems
echo -e "\e ${BCyan} === Type Password for fems:${FEMS} === \e[0m"
passwd fems
echo $FEMS | tee /etc/hostname
/bin/sed "s/\(127.0.1.1.*\)techbase.*/\1$FEMS/" /etc/hosts --in-place; cat /etc/hosts
echo "apikey=${APIKEY}" | tee /etc/fems
apt install aptitude --assume-yes
apt update --assume-yes
apt-mark hold raspberrypi-bootloader raspberrypi-kernel raspberrypi-sys-mods
for pkg in $(apt-mark showhold); do aptitude hold $pkg; done && aptitude search ~ahold
echo -e "\e ${BCyan} === Systemd-Networkd aktivieren === \e[0m"
echo "[Match]
Name=eth0

[Network]
DHCP=yes
LinkLocalAddressing=yes
Address=192.168.100.100/24" | tee /etc/systemd/network/eth0.network &&

echo "[Match]
Name=eth1

[Network]
Address=10.4.0.1/16
Address=192.168.0.2/27
Address=192.168.1.9/26" | tee /etc/systemd/network/eth1.network
systemctl daemon-reload
systemctl disable --now ifupdown dhcpcd dhcpcd5 isc-dhcp-client isc-dhcp-common rsyslog
apt --autoremove purge ifupdown dhcpcd dhcpcd5 isc-dhcp-client isc-dhcp-common rsyslog --assume-yes
rm -r /etc/network /etc/dhcp
systemctl disable --now avahi-daemon libnss-mdns
apt --autoremove purge avahi-daemon --assume-yes
systemctl enable systemd-networkd.service systemd-resolved.service
apt install libnss-resolve --assume-yes
wget -O - http://fenecon.de/debian/fems.gpg.key | apt-key add -
wget http://fenecon.de/debian/fems.list -O /etc/apt/sources.list.d/fems.list
apt-key adv --recv-key --keyserver keyserver.ubuntu.com 8B48AD6246925553
apt-key adv --recv-key --keyserver keyserver.ubuntu.com 7638D0442B90D010
apt update --assume-yes

echo -e "\e ${BCyan} === OpenEMS Installieren === \e[0m"
apt-get install fonts-dejavu libasound2 --assume-yes
apt --fix-broken install --assume-yes
journalctl -u openems -n 20 --no-pager && wget https://fenecon.de/fems-download/update-fems.sh -O /tmp/update-fems.sh --no-check-certificate && bash /tmp/update-fems.sh

echo -e "\e ${BCyan} === fems-autoupdate deaktivieren === \e[0m"
dpkg-divert /usr/bin/fems-autoupdate
echo  "exit" >> /usr/bin/fems-autoupdate
echo  "
             ███████╗███████╗███╗   ███╗███████╗
             ██╔════╝██╔════╝████╗ ████║██╔════╝
             █████╗  █████╗  ██╔████╔██║███████╗
             ██╔══╝  ██╔══╝  ██║╚██╔╝██║╚════██║
             ██║     ███████╗██║ ╚═╝ ██║███████║
             ╚═╝     ╚══════╝╚═╝     ╚═╝╚══════╝
              FENECON Energy Management System

" | tee /etc/motd

echo -e "\e ${BCyan} === DONE === \e[0m"

	# Preparation for reboot
	script="bash /tmp/test_techbase_cm4_install.sh"
  
	# add this script to zsh so it gets triggered immediately after reboot
	# change it to .bashrc if using bash shell
	echo "$script" >> ~/.bashrc 
  
	# create a flag file to check if we are resuming from reboot.
	sudo touch /var/run/resume-after-reboot
  
	echo -e "\e ${BCyan} === rebooting.. ==== \e[0m"
#reboot
shutdown -r now

else 
  echo -e "\e ${BCyan} === resuming script after reboot... === \e[0m "
  
  # Remove the line that we added in zshrc
  sed -i '/bash/d' ~/.zshrc 
  
  # remove the temporary file that we created to check for reboot
  sudo rm -f /var/run/resume-after-reboot

# install jdk 16
if ! dpkg -s adoptopenjdk-16-hotspot-jre >/dev/null 2>&1; then
	echo "# Install 'fonts-dejavu, libasound2'"	
	apt-get install fonts-dejavu libasound2 --assume-yes --force-yes
	echo "# Install 'adoptopenjdk-16-hotspot-jre'"
	wget http://fenecon.de/fems-download/deb/adoptopenjdk-16-hotspot-jre_16.0.1+9-3_armhf.deb -O /tmp/adoptopenjdk-16.deb --no-check-certificate
	dpkg -i /tmp/adoptopenjdk-16.deb
fi

if dpkg -s openjdk-8-jre-headless >/dev/null 2>&1; then
	echo "# Remove 'openjdk-8-jre-headless'"
	apt-get remove --assume-yes openjdk-8-jre-headless
fi

	history -c 
	rm /root/.bash_history 
	passwd -dl root
	service_port_ctrl off && comctrl 1 RS-485 2 RS-485 && service_port_ctrl status && comctrl -r
	wget http://fenecon.de/debian-test/setup-fems.sh -O /tmp/setup-fems.sh && bash /tmp/setup-fems.sh
	systemctl restart openems --no-block; journalctl -lfu openems
fi
exit
