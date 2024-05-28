#!/bin/bash

set -o errexit
#set -x

echo "  _____ _____ __  __ ____          ___     ___   _ _____ _____            _               "
echo " |  ___| ____|  \/  / ___|        / \ \   / / \ | | ____|_   _|  ___  ___| |_ _   _ _ __  "
echo " | |_  |  _| | |\/| \___ \ _____ / _ \ \ / /|  \| |  _|   | |   / __|/ _ \ __| | | | '_ \ "
echo " |  _| | |___| |  | |___) |_____/ ___ \ V / | |\  | |___  | |   \__ \  __/ |_| |_| | |_) |"
echo " |_|   |_____|_|  |_|____/     /_/   \_\_/  |_| \_|_____| |_|   |___/\___|\__|\__,_| .__/ "
echo "                                                                                   |_|    "
echo ""

if [ -z ${FEMS+x} ]; then 
	echo "Variable FEMS muss gesetzt sein! z. B. FEMS=fems123"
fi

if [ -z ${APIKEY+x} ]; then 
	echo "Variable APIKEY muss gesetzt sein! z. B. APIKEX=abcdef"
fi

if [ -z ${PASSWORD+x} ]; then 
	echo "Variable PASSWORD muss gesetzt sein! z. B. PASSWORD=geheim"
fi

echo
echo "# $FEMS: Erstelle User fems"
useradd --comment FEMS --groups sudo,root --shell /bin/bash --create-home fems || true

echo
echo "# $FEMS: Setze Passwort"
echo "fems:$PASSWORD" | chpasswd

echo
echo "# $FEMS: Aktiviere SSH"
systemctl enable ssh
systemctl start ssh

echo
echo "# $FEMS: Setze Hostname"
echo $FEMS | tee /etc/hostname
/bin/sed "s/\(127.0.1.1.*\)raspberrypi.*/\1$FEMS/" /etc/hosts --in-place; cat /etc/hosts

echo
echo "# $FEMS: Setze Apikey"
echo "apikey=${APIKEY}" | tee /etc/fems

echo
echo "# $FEMS: Aktualisiere Paket-Pinning für 'aptitude'"
for pkg in $(apt-mark showhold); do aptitude hold $pkg; done
aptitude search ~ahold

echo
echo "# $FEMS: Entferne unnötige Pakete und Services"
apt -y --autoremove purge openresolv ifupdown dhcpcd5 isc-dhcp-client isc-dhcp-common
systemctl disable attinyupdate.service || true
systemctl disable ledservice.service || true
systemctl disable quectel.service || true
systemctl disable smartedgehalt.service || true
systemctl disable restservice.service || true
systemctl stop attinyupdate.service || true
systemctl stop ledservice.service || true
systemctl stop quectel.service || true
systemctl stop smartedgehalt.service || true
systemctl stop restservice.service || true
rm -f /etc/systemd/system/attinyupdate.service
rm -f /etc/systemd/system/ledservice.service
rm -f /etc/systemd/system/quectel.service
rm -f /etc/systemd/system/smartedgehalt.service
rm -f /etc/systemd/system/restservice.service
systemctl daemon-reload
sed --in-place '/systemctl stop \(ledservice\|smartedgehalt\)/d' /opt/avnet-iot/iotservices/reboot
sed --in-place '/echo.*> \/sys\/class\/leds/d' /opt/avnet-iot/iotservices/reboot

echo
echo "# $FEMS: Installiere FEMS-Pakete"
wget -O - http://fenecon.de/debian/fems.gpg.key | apt-key add -
wget http://fenecon.de/debian/fems.list -O /etc/apt/sources.list.d/fems.list
apt update
apt -y install vim at ntpdate ufw screen python-socksipychain python-urllib3 python-pymodbus nginx-light lsof openjdk-8-jre-headless debian-keyring dirmngr
apt-key adv --recv-key --keyserver keyserver.ubuntu.com 8B48AD6246925553
apt-key adv --recv-key --keyserver keyserver.ubuntu.com 7638D0442B90D010
wget http://fenecon.de/debian-test/openems_core-latest.deb -O /tmp/openems_core-latest.deb &&
wget http://fenecon.de/debian-test/openems_core_fems-latest.deb -O /tmp/openems_core_fems-latest.deb &&
dpkg -i /tmp/openems_core-latest.deb /tmp/openems_core_fems-latest.deb &&
rm -f /tmp/openems_core-latest.deb && rm -f /tmp/openems_core_fems-latest.deb

echo
echo "# $FEMS: Setze Login-Prompt"
echo "
             ███████╗███████╗███╗   ███╗███████╗
             ██╔════╝██╔════╝████╗ ████║██╔════╝
             █████╗  █████╗  ██╔████╔██║███████╗
             ██╔══╝  ██╔══╝  ██║╚██╔╝██║╚════██║
             ██║     ███████╗██║ ╚═╝ ██║███████║
             ╚═╝     ╚══════╝╚═╝     ╚═╝╚══════╝
              FENECON Energy Management System
" | tee /etc/motd

echo
echo "# $FEMS: Update aller Pakete"
fems-autoupdate

echo
echo "# $FEMS: Aktiviere systemd-networkd Konfiguration"
rm -rf /etc/network/interfaces /etc/network/interfaces.d /etc/resolvconf.conf
echo "auto lo
iface lo inet loopback" | tee /etc/network/interfaces

echo resolvconf=NO | tee /etc/resolvconf.conf

echo "[Match]
Name=eth0

[Network]
DHCP=yes
LinkLocalAddressing=yes
Address=192.168.100.100/24" | tee /etc/systemd/network/eth0.network &&

echo "[Match]
Name=eth1

[Network]
Address=10.4.0.1/16" | tee /etc/systemd/network/eth1.network

systemctl mask networking.service
systemctl mask dhcpcd.service

systemctl enable systemd-networkd.service
systemctl enable systemd-resolved.service
ln -sf /run/systemd/resolve/resolv.conf /etc/resolv.conf

systemctl start systemd-resolved

echo
echo "# $FEMS: Aktiviere systemd-networkd und starte neu!"
systemctl start systemd-networkd && reboot

