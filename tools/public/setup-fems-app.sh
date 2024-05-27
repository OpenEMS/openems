#!/bin/bash
#
# Script to automatically setup a FEMS.

set -e

# Define environment variables
BASE_PATH="/etc/openems.d"
ETC_FEMS="/etc/fems"

CONF_MODBUS10_TCP_192_168_1_199="Bridge/Modbus/Tcp/1dbcdef5-c170-42f8-ba8f-0636f0e92525.config"
CONF_IO0_KMTRONIC="IO/KMtronic/e420f489-8032-4b6c-a3d8-564eecbed229.config"

# TODO: Die Meisten Apps ändern sobald ein funktionierendes Spezialwidget dafür existiert.
CONF_WAERMEPUMPE_CTRL_CHANNEL_THRESHOLD_2="Controller/ChannelThreshold/60c05a64-fc14-4d16-a74c-3ac4f09f2a9e.config"
CONF_WAERMEPUMPE_CTRL_CHANNEL_THRESHOLD_3="Controller/ChannelThreshold/1bded5f3-1533-4a56-a825-50e1fe99308c.config"

CONF_HEIZSTAB_CTRL_CHANNEL_SINGLE_THRESHOLD_4="Controller/IO/ChannelSingleThreshold/b1c2e3e1-e7f2-4725-b4bc-6c5686211db5.config"
CONF_HEIZSTAB_CTRL_CHANNEL_SINGLE_THRESHOLD_5="Controller/IO/ChannelSingleThreshold/6fcf4f6f-5638-45b8-9722-4b3c0ed5c2f6.config"
CONF_HEIZSTAB_CTRL_CHANNEL_SINGLE_THRESHOLD_6="Controller/IO/ChannelSingleThreshold/a10571db-2188-4b61-aae3-cca6798b6c9e.config"

CONF_HEIZSTAB_CTRL_HEATING_ELEMENT="Controller/IO/HeatingElement/81241275-4404-45e3-88bd-587a62b5dbe5.config"

CONF_LADESTATION_KEBA_0="Evcs/Keba/KeContact/82703bbf-dc49-4620-afc1-c2161a0f0528.config"
CONF_LADESTATION_CTRL_0="Controller/Evcs/f4df0c42-e04d-419b-9e31-95185611dfdc.config"

#
# Setup Bridge Modbus/TCP modbus10
#
function setup_modbus10_tcp_192_168_1_199 {
	echo "# Creating $CONF_MODBUS10_TCP_192_168_1_199"
	mkdir -p "$(dirname $CONF_MODBUS10_TCP_192_168_1_199)"
	cat >$CONF_MODBUS10_TCP_192_168_1_199 <<EOL
alias="FEMS-Relais"
enabled=B"true"
id="modbus10"
ip="192.168.1.199"
logVerbosity="NONE"
service.factoryPid="Bridge.Modbus.Tcp"
service.pid="Bridge.Modbus.Tcp.1dbcdef5-c170-42f8-ba8f-0636f0e92525"
EOL
	
	if [ "$(grep -c 'Address=192.168.1.100/24' /etc/systemd/network/eth0.network)" -eq "0" ]; then 
		echo "# Konfiguriere IP-Adresse 192.168.1.100/24"
		echo "Address=192.168.1.100/24" >> /etc/systemd/network/eth0.network
		systemctl restart systemd-networkd
		systemctl enable systemd-networkd
	else
		echo "# IP-Adresse 192.168.1.100/24 ist bereits konfiguriert"
	fi
}

#
# Setup KMTronic Relais Board -> modbus10
#
function setup_io0_kmtronic {
	echo "# Creating $CONF_IO0_KMTRONIC"
	mkdir -p "$(dirname $CONF_IO0_KMTRONIC)"
	cat >$CONF_IO0_KMTRONIC <<EOL
alias="Relais Board"
enabled=B"true"
id="io0"
modbus.id="modbus10"
modbusUnitId=I"1"
service.factoryPid="IO.KMtronic"
service.pid="IO.KMtronic.e420f489-8032-4b6c-a3d8-564eecbed229"
EOL
}

#
# Setup FEMS-App Wärmepumpe an io0 Relais Board
#
function setup_waermepumpe_kmtronic {
	echo "# Creating $CONF_WAERMEPUMPE_CTRL_CHANNEL_THRESHOLD_2"
	mkdir -p "$(dirname $CONF_WAERMEPUMPE_CTRL_CHANNEL_THRESHOLD_2)"
	cat >$CONF_WAERMEPUMPE_CTRL_CHANNEL_THRESHOLD_2 <<EOL
alias="Wärmepumpe\ Relay2"
enabled=B"true"
highThreshold=I"40"
hysteresis=I"5"
id="ctrlChannelThreshold2"
inputChannelAddress="_sum/EssSoc"
invert=B"false"
lowThreshold=I"0"
outputChannelAddress="io0/Relay2"
service.factoryPid="Controller.ChannelThreshold"
service.pid="Controller.ChannelThreshold.60c05a64-fc14-4d16-a74c-3ac4f09f2a9e"
EOL
	echo "# Creating $CONF_WAERMEPUMPE_CTRL_CHANNEL_THRESHOLD_3"
	mkdir -p "$(dirname $CONF_WAERMEPUMPE_CTRL_CHANNEL_THRESHOLD_3)"
	cat >$CONF_WAERMEPUMPE_CTRL_CHANNEL_THRESHOLD_3 <<EOL
alias="Wärmepume\ Relay3"
enabled=B"true"
highThreshold=I"100"
hysteresis=I"5"
id="ctrlChannelThreshold3"
inputChannelAddress="_sum/EssSoc"
invert=B"false"
lowThreshold=I"80"
outputChannelAddress="io0/Relay3"
service.factoryPid="Controller.ChannelThreshold"
service.pid="Controller.ChannelThreshold.1bded5f3-1533-4a56-a825-50e1fe99308c"
EOL
}

#
# Setup FEMS-App Heizstab an io0 Relais Board
#
function setup_heizstab_kmtronic {
	echo "# Creating $CONF_HEIZSTAB_CTRL_HEATING_ELEMENT"
	mkdir -p "$(dirname $CONF_HEIZSTAB_CTRL_HEATING_ELEMENT)"
	cat >$CONF_HEIZSTAB_CTRL_HEATING_ELEMENT <<EOL
alias="Heizstab"
defaultLevel="LEVEL_1"
enabled=B"true"
endTime="17:00"
id="ctrlIoHeatingElement0"
minTime=I"1"
minimumSwitchingTime=I"60"
mode="AUTOMATIC"
outputChannelPhaseL1="io0/Relay4"
outputChannelPhaseL2="io0/Relay5"
outputChannelPhaseL3="io0/Relay6"
powerPerPhase=I"2000"
service.factoryPid="Controller.IO.HeatingElement"
service.pid="Controller.IO.HeatingElement.81241275-4404-45e3-88bd-587a62b5dbe5"
workMode="NONE"
EOL
}

#
# Setup FEMS-App "Alter" Heizstab (= 3 x Singlethreshold) an io0 Relais Board
#
function setup_alter_heizstab_kmtronic {
	echo "# Creating $CONF_HEIZSTAB_CTRL_CHANNEL_SINGLE_THRESHOLD_4"
	mkdir -p "$(dirname $CONF_HEIZSTAB_CTRL_CHANNEL_SINGLE_THRESHOLD_4)"
	cat >$CONF_HEIZSTAB_CTRL_CHANNEL_SINGLE_THRESHOLD_4 <<EOL
alias="Heizstab\ 1"
enabled="true"
id="ctrlChannelSingleThreshold4"
inputChannelAddress="_sum/EssSoc"
invert="false"
minimumSwitchingTime="60"
mode="AUTOMATIC"
outputChannelAddress="io0/Relay4"
service.factoryPid="Controller.IO.ChannelSingleThreshold"
service.pid="Controller.IO.ChannelSingleThreshold.b1c2e3e1-e7f2-4725-b4bc-6c5686211db5"
switchedLoadPower="0"
threshold=I"92"
EOL
	echo "# Creating $CONF_HEIZSTAB_CTRL_CHANNEL_SINGLE_THRESHOLD_5"
	mkdir -p "$(dirname $CONF_HEIZSTAB_CTRL_CHANNEL_SINGLE_THRESHOLD_5)"
	cat >$CONF_HEIZSTAB_CTRL_CHANNEL_SINGLE_THRESHOLD_5  <<EOL
alias="Heizstab\ 2"
enabled="true"
id="ctrlChannelSingleThreshold5"
inputChannelAddress="_sum/EssSoc"
invert="false"
minimumSwitchingTime="60"
mode="AUTOMATIC"
outputChannelAddress="io0/Relay5"
service.factoryPid="Controller.IO.ChannelSingleThreshold"
service.pid="Controller.IO.ChannelSingleThreshold.6fcf4f6f-5638-45b8-9722-4b3c0ed5c2f6"
switchedLoadPower="0"
threshold=I"94"
EOL
	echo "# Creating $CONF_HEIZSTAB_CTRL_CHANNEL_SINGLE_THRESHOLD_6"
	mkdir -p "$(dirname $CONF_HEIZSTAB_CTRL_CHANNEL_SINGLE_THRESHOLD_6)"
	cat >$CONF_HEIZSTAB_CTRL_CHANNEL_SINGLE_THRESHOLD_6  <<EOL
alias="Heizstab\ 3"
enabled="true"
id="ctrlChannelSingleThreshold6"
inputChannelAddress="_sum/EssSoc"
invert="false"
minimumSwitchingTime="60"
mode="AUTOMATIC"
outputChannelAddress="io0/Relay6"
service.factoryPid="Controller.IO.ChannelSingleThreshold"
service.pid="Controller.IO.ChannelSingleThreshold.a10571db-2188-4b61-aae3-cca6798b6c9e"
switchedLoadPower="0"
threshold=I"96"
EOL
}


#
# Setup KEBA Ladestation evcs0
#
function setup_ladestation_keba_evcs0 {
	echo "# Creating $CONF_LADESTATION_KEBA_0"
	mkdir -p "$(dirname $CONF_LADESTATION_KEBA_0)"
	cat >$CONF_LADESTATION_KEBA_0 <<EOL
alias="Ladestation"
debugMode=B"false"
enabled=B"true"
id="evcs0"
ip="192.168.25.11"
minHwCurrent="6000"
service.factoryPid="Evcs.Keba.KeContact"
service.pid="Evcs.Keba.KeContact.82703bbf-dc49-4620-afc1-c2161a0f0528"
EOL

	if [ "$(grep -c 'Address=192.168.25.1/24' /etc/systemd/network/eth0.network)" -eq "0" ]; then 
		echo "# Konfiguriere IP-Adresse 192.168.25.1/24"
		echo "Address=192.168.25.1/24" >> /etc/systemd/network/eth0.network
		systemctl restart systemd-networkd
		systemctl enable systemd-networkd
	else
		echo "# IP-Adresse 192.168.25.1/24 ist bereits konfiguriert"
	fi
}

#
# Setup EVCS Ladestation Controller -> evcs0
#
function setup_ladestation_controller_evcs0 {
	echo "# Creating $CONF_LADESTATION_CTRL_0"
	mkdir -p "$(dirname $CONF_LADESTATION_CTRL_0)"
	cat >$CONF_LADESTATION_CTRL_0 <<EOL

alias="Ladestation Controller"
chargeMode="FORCE_CHARGE"
debugMode=B"false"
defaultChargeMinPower=I"0"
enabled=B"true"
enabledCharging=B"true"
energySessionLimit=I"0"
ess.id="ess0"
evcs.id="evcs0"
evcs.target="(&(enabled\=true)(!(service.pid\=Controller.Evcs.f4df0c42-e04d-419b-9e31-95185611dfdc))(|(id\=evcs0)))"
forceChargeMinPower=I"7360"
id="ctrlEvcs0"
priority="CAR"
service.factoryPid="Controller.Evcs"
service.pid="Controller.Evcs.f4df0c42-e04d-419b-9e31-95185611dfdc"
EOL
}


#
# Setup start that we are always in the right path
#
function setup_start {
	cd $BASE_PATH
}

function setup_kmtronic_tcp {
	setup_modbus10_tcp_192_168_1_199
	setup_io0_kmtronic
}

function setup_waermepumpe_kmtronic_tcp {
	setup_start
	setup_kmtronic_tcp
	setup_waermepumpe_kmtronic
	systemctl restart openems --no-block; journalctl -lfu openems
}

function setup_heizstab_kmtronic_tcp {
	setup_start
	setup_kmtronic_tcp
	setup_heizstab_kmtronic
	systemctl restart openems --no-block; journalctl -lfu openems
}

function setup_alter_heizstab_kmtronic_tcp {
	setup_start
	setup_kmtronic_tcp
	setup_alter_heizstab_kmtronic
	systemctl restart openems --no-block; journalctl -lfu openems
}

function setup_ladestation_keba_mit_controller {
	setup_start
	setup_ladestation_keba_evcs0
	setup_ladestation_controller_evcs0
	systemctl restart openems --no-block; journalctl -lfu openems
}

function install_latest {
	wget http://fenecon.de/debian-test/openems_core-latest.deb -O /tmp/openems_core-latest.deb \
	&& wget http://fenecon.de/debian-test/openems_core_fems-latest.deb -O /tmp/openems_core_fems-latest.deb \
	&& dpkg -i /tmp/openems_core-latest.deb /tmp/openems_core_fems-latest.deb \
	&& rm /tmp/openems_core-latest.deb \
	&& rm /tmp/openems_core_fems-latest.deb \
	&& systemctl restart openems \
	&& wget http://fenecon.de/debian-test/setup-fems-app.sh -O /tmp/setup-fems-app.sh && bash /tmp/setup-fems-app.sh
}
#20 45 12
function choose_setup {
	CHOICE=$(whiptail --menu "Select:" 20 70 12 \
		"Update von Openems(Vor dem Installieren der ersten App ausführen!)" "" \
		"FEMS-App Waermepumpe (mit KMTronic/TCP)" "" \
		"FEMS-App Heizstab (mit KMTronic/TCP)" "" \
		"FEMS-App "Alter" Heizstab (= 3 x Singlethreshold) (mit KMTronic/TCP)" "" \
		"FEMS-App KEBA Ladestation (KEBA und Controller)" "" \
		3>&1 1>&2 2>&3)

	case $CHOICE in
		"Update von Openems(Vor dem Installieren der ersten App ausführen!)")			install_latest;;
		"FEMS-App Waermepumpe (mit KMTronic/TCP)")	setup_waermepumpe_kmtronic_tcp;;
		"FEMS-App Heizstab (mit KMTronic/TCP)")	setup_heizstab_kmtronic_tcp;;
		"FEMS-App "Alter" Heizstab (= 3 x Singlethreshold) (mit KMTronic/TCP)")	setup_alter_heizstab_kmtronic_tcp;;
		"FEMS-App KEBA Ladestation (KEBA und Controller)")	setup_ladestation_keba_mit_controller;;
	esac
}

choose_setup