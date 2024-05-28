#!/bin/bash
#
# Script to automatically updates a ATL Battery.

# wget http://fenecon.de/debian-test/atlUpdate/atlUpdate.sh -O /tmp/atlUpdate.sh && bash /tmp/atlUpdate.sh /dev/busUSB1 19200 atl.bin false

# Force Update
# wget http://fenecon.de/debian-test/atlUpdate/atlUpdate.sh -O /tmp/atlUpdate.sh && bash /tmp/atlUpdate.sh /dev/busUSB1 19200 atl.bin true 


# Only for testing: java -jar atlUpdate.jar /dev/busUSB2 19200 atl.bin
# java -jar JARNAME.jar PORTNAME BAUDRATE PATH_of_the_UPDATE_FILE boolean_force_update(true or false)>
# java -jar UpdateV2.5.5.jar ttyUSB0 19200 c:\home\udpatefile.bin false

set -e

PORT=$1
BAUD_RATE=$2
ATL_FW=$3
FORCE_MODE=$4


echo "Stop Openems"
systemctl stop openems --no-block;

echo "Main-Class: j2mod.App.class" > /tmp/input-file

echo "Start update script"	
wget http://fenecon.de/debian-test/atlUpdate/AtlUpdate.jar -O /tmp/AtlUpdate.jar
wget http://fenecon.de/debian-test/atlUpdate/$ATL_FW -O /tmp/$ATL_FW
java -jar /tmp/AtlUpdate.jar $PORT $BAUD_RATE /tmp/$ATL_FW $FORCE_MODE

echo "Restart Openems"
systemctl restart openems --no-block;

echo ""
echo ""
echo "# #################################################"
echo "# ################# FINISHED ######################"
echo "# #################################################"




