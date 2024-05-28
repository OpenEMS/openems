#!/bin/bash
#
# Script to automatically updates a GoodWe Inverter.
#
# Please exchange the Type and the Version to install different Firmware for different GoodWe types
#
# Example (GoodWe10, 09_03_24) in command-line:
# wget http://fenecon.de/debian-test/goodweUpdate/mainGoodweUpdate.sh -O /tmp/mainGoodweUpdate.sh && bash -x /tmp/mainGoodweUpdate.sh goodwe10 09_03_24 /dev/busUSB2 9600
#
# Monitoring:
# echo "wget http://fenecon.de/debian-test/goodweUpdate/mainGoodweUpdate.sh -O /tmp/mainGoodweUpdate.sh && bash -x /tmp/mainGoodweUpdate.sh goodwe10 09_03_24 /dev/busUSB2 9600 > /tmp/goodweLog 2>& 1" | at now
#
#
#
#
# Example (GoodWe20 or 30, 14_04_24):
# wget http://fenecon.de/debian-test/goodweUpdate/mainGoodweUpdate.sh -O /tmp/mainGoodweUpdate.sh && bash -x /tmp/mainGoodweUpdate.sh goodwe20_30 14_04_24 /dev/busUSB2 9600
#
# echo "wget http://fenecon.de/debian-test/goodweUpdate/mainGoodweUpdate.sh -O /tmp/mainGoodweUpdate.sh && bash -x /tmp/mainGoodweUpdate.sh goodwe20_30 14_04_24 /dev/busUSB2 9600 > /tmp/goodweLog 2>& 1" | at now


# Log for Monitoring: tail /tmp/goodweLog

#set -e exit on error

TYPE=$1
VERSION=$2
PORT=$3
BAUD_RATE=$4


function downloadGoodweUpdate() {

	echo "Download GoodWe Update"	
	wget http://fenecon.de/debian-test/goodweUpdate/$TYPE/goodweUpdate.jar -O /tmp/goodweUpdate.jar
	if [ $? -ne 0 ]; then return 8; fi
	
	echo "Giving executive rights"	
	chmod +x /tmp/goodweUpdate.jar
	if [ $? -ne 0 ]; then return 8; fi
	
	echo "Download GoodWe ARM Firmware"	
	wget http://fenecon.de/debian-test/goodweUpdate/$TYPE/$VERSION/arm.bin -O /tmp/arm.bin
	if [ $? -ne 0 ]; then return 8; fi
	
	echo "Download GoodWe DSP Firmware"	
	wget http://fenecon.de/debian-test/goodweUpdate/$TYPE/$VERSION/dsp.bin -O /tmp/dsp.bin
	if [ $? -ne 0 ]; then return 8; fi
}

function goodWeUpdate() {
	echo "Starte GoodWe Update"	
	java -jar /tmp/goodweUpdate.jar $PORT $BAUD_RATE /tmp/arm.bin /tmp/dsp.bin
}

function restartOpenEMS() {
	echo "Restart Openems"
	systemctl restart openems --no-block;
}

function finally() {
	restartOpenEMS;
	exit
	echo "Should not be visable"
}

update() {
	
	echo "Start GoodWe Update"
	downloadGoodweUpdate;
	if [ $? -ne 0 ];
	then
		echo "GoodWe Update fehlgeschlagen! Bitte erneut versuchen! \nBitte pruefe ob das FEMS Zugang zum Internet hat"
		exit
		echo "Should not be visable 2"
	fi
	
	echo "Stop Openems"
	systemctl stop openems --no-block;
	
	goodWeUpdate;
	if [ $? -ne 0 ];
	then
		echo "GoodWe Update fehlgeschlagen! Bitte erneut versuchen! \nWenn fehler erneut auftritt, bitte pruefe ob der PORT dem Standard /dev/busUSB2 entspricht (Bei unklarheit, um Hilfe fragen) \nStelle zusaetzlich sicher, dass eine Verbindung zum Wechselrichter gegeben ist."
		finally;
		echo "Should not be visable 3"
		return;
	fi

	restartOpenEMS;
	if [ $? -ne 0 ];
	then
		echo "OpenEMS-Restart fehlgeschlagen"
		finally;
		echo "Should not be visable 4"
		return;
	fi

	echo ""
	echo ""
	echo "# #################################################"
	echo "# ################# FINISHED ######################"
	echo "# #################################################"
}

update;
finally;



