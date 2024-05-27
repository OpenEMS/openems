#!/bin/bash
#
# Script to automatically updates a GoodWe 20/30 Inverter. The arm and dsp files are already in the reprepro.

# Call:
# wget http://fenecon.de/debian-test/goodweUpdate/goodweUpdate20_30_test.sh -O /tmp/goodweUpdate.sh && bash /tmp/goodweUpdate.sh /dev/busUSB2 9600 


set -e

PORT=$1
BAUD_RATE=$2
ARM_FW="arm.bin"
DSP_FW="dsp.bin"

echo "Stop Openems"
systemctl stop openems --no-block;

echo "Start update script"	
wget http://fenecon.de/debian-test/goodweUpdate/goodwe20_30_test/goodweUpdate.jar -O /tmp/goodweUpdate.jar
wget http://fenecon.de/debian-test/goodweUpdate/goodwe20_30_test/arm.bin -O /tmp/$ARM_FW 
wget http://fenecon.de/debian-test/goodweUpdate/goodwe20_30_test/dsp.bin -O /tmp/$DSP_FW
java -jar /tmp/goodweUpdate.jar $PORT $BAUD_RATE /tmp/$ARM_FW /tmp/$DSP_FW

echo "Restart Openems"
systemctl restart openems --no-block;

echo ""
echo ""
echo "# #################################################"
echo "# ################# FINISHED ######################"
echo "# #################################################"




