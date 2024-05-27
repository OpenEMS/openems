#!/bin/bash
#
# Script to automatically updates a GoodWe Inverter.

# wget http://fenecon.de/debian-test/goodweUpdate/goodweUpdate.sh -O /tmp/goodweUpdate.sh && bash /tmp/goodweUpdate.sh /dev/busUSB2 9600 arm.bin dsp.bin
# wget http://fenecon.de/debian-test/goodweUpdate/goodweUpdate.sh -O /tmp/goodweUpdate.sh && bash /tmp/goodweUpdate.sh /dev/busUSB2 9600 410-02041-23_237.bin 410-04029-09_v164.bin

# Only for testing: java -jar goodweUpdate.jar /dev/busUSB2 9600 arm.bin dsp.bin

set -e

PORT=$1
BAUD_RATE=$2
ARM_FW=$3
DSP_FW=$4


# Check input parameters
if [ -z $3 ]
then
	# Take default (Austria version of the server)
	wget http://fenecon.de/debian-test/goodweUpdate/arm.bin -O /tmp/arm.bin
    ARM_FW="/tmp/arm.bin"
else
      echo "\$3 is NOT empty"
fi

if [ -z $4 ]
then
	# Take default (Austria version of the server)
	wget http://fenecon.de/debian-test/goodweUpdate/dsp.bin -O /tmp/dsp.bin
	DSP_FW="/tmp/dsp.bin"
else
      echo "\$4 is NOT empty"
fi
	
if [ ! -f $ARM_FW ]; then
    echo "$ARM_FW existiert nicht in auf diesem System."
    echo "Willst du die Standard Firmware updaten, gib bitte arm.bin & dsp.bin im Kommando nicht mehr mit an!"
	
	echo -e "Versuche: \nwget http://fenecon.de/debian-test/goodweUpdate/goodweUpdate.sh -O /tmp/goodweUpdate.sh && bash /tmp/goodweUpdate.sh /dev/busUSB2 9600"

	exit
else 
    echo "$3 used"
	cp $ARM_FW /tmp/arm.bin
fi
if [ ! -f $DSP_FW ]; then
	echo "$DSP_FW existiert nicht in auf diesem System."
    echo "Willst du die Standard Firmware updaten, gib bitte arm.bin & dsp.bin im Kommando nicht mehr mit an!"
	
	echo -e "Versuche: \nwget http://fenecon.de/debian-test/goodweUpdate/goodweUpdate.sh -O /tmp/goodweUpdate.sh && bash /tmp/goodweUpdate.sh /dev/busUSB2 9600"
	exit
else 
    echo "$4 used"
	cp $DSP_FW /tmp/dsp.bin
fi




# Start process
echo "Stop Openems"
systemctl stop openems --no-block;

echo "Start update script"	
wget http://fenecon.de/debian-test/goodweUpdate/goodweUpdate.jar -O /tmp/goodweUpdate.jar
java -jar /tmp/goodweUpdate.jar $PORT $BAUD_RATE /tmp/arm.bin /tmp/dsp.bin

echo "Restart Openems"
systemctl restart openems --no-block;

echo ""
echo ""
echo "# #################################################"
echo "# ################# FINISHED ######################"
echo "# #################################################"




