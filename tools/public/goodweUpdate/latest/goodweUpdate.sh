#!/bin/bash
#
# Script to automatically updates a GoodWe Inverter.

# wget http://fenecon.de/debian-test/goodweUpdate/goodweUpdate.sh -O /tmp/goodweUpdate.sh && bash /tmp/goodweUpdate.sh /dev/busUSB2 9600 arm.bin dsp.bin
# wget http://fenecon.de/debian-test/goodweUpdate/goodweUpdate.sh -O /tmp/goodweUpdate.sh && bash /tmp/goodweUpdate.sh /dev/busUSB2 9600 410-02041-23_237.bin 410-04029-09_v164.bin

# Only for testing: java -jar goodweUpdate.jar /dev/busUSB2 9600 arm.bin dsp.bin

# TODO: Alles zu einem Shell script zusammenfügen

set -e

PORT=$1
BAUD_RATE=$2
ARM_FW=$3
DSP_FW=$4


if [ -z "$3" ]
then
      echo "\$3 is empty"
else
      echo "\$3 is NOT empty"
fi



echo "Stop Openems"
systemctl stop openems --no-block;

echo "Start update script"	
wget http://fenecon.de/debian-test/goodweUpdate/goodweUpdate.jar -O /tmp/goodweUpdate.jar
wget http://fenecon.de/debian-test/goodweUpdate/latest/arm.bin -O /tmp/arm.bin
wget http://fenecon.de/debian-test/goodweUpdate/latest/dsp.bin -O /tmp/dsp.bin
java -jar /tmp/goodweUpdate.jar $PORT $BAUD_RATE /tmp/$ARM_FW /tmp/$DSP_FW

echo "Restart Openems"
systemctl restart openems --no-block;

echo ""
echo ""
echo "# #################################################"
echo "# ################# FINISHED ######################"
echo "# #################################################"




