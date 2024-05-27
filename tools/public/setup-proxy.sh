#!/bin/bash -e

if [ -z "$( cat /proc/version | grep rockchip )" ];
then
	echo "NO PROXY!!";
	exit;
fi;

FIONA1_IP=10.16.220.1
FIOIA2_IP=10.16.220.2

while true; do
        read -p "which FIONA? (1/2) " fiona
        case $fiona in
                [1] ) break;;
                [2] ) break;;
                * ) echo "'$fiona' is invalid!";;
        esac;
done;

while true; do
        read -p "which PORT? (1-10) " port
        case $port in
                [123456789] ) break;;
                "10" ) break;;
                * ) echo "'$port' is invalid!";;
        esac;
done;

echo "setup Port$port for Fiona$fiona";

proxyName=proxy$(printf -v port "%02d" $port ; echo $port)
proxyIp=$(if [ "$fiona" == "1" ]; then echo $FIONA1_IP; else echo $FIOIA2_IP; fi;)$((port+10))


### SET HOSTNAME ###

TXT_BOLD="\033[1m";
TXT_DEFAULT="\033[0m";

current=$(cat /etc/hostname)
new=$proxyName

sed -i "s|$current|$new|g" /etc/hostname /etc/hosts;


### SET IP ###

ip=$proxyIp;
gateway=10.16.220.1;

if [ -z "$ip" ] || [ -z "$gateway" ];
then
        echo "requires 2 arguments ip and gateway";
fi;

cat << EOF | sudo tee /etc/systemd/network/10-eth0.network
[Match]
Name=end0

[Network]
DHCP=yes
LinkLocalAddressing=yes
Address=192.168.100.101/24
Address=$ip/24

[DHCP]
RouteMetric=512

[Route]
Gateway=$gateway
Metric=1024
EOF
