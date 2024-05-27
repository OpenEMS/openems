#!/bin/bash
#
# Script to automatically updates a GoodWe 20/30 Inverter. The arm and dsp files are already in the reprepro.

# Call:
# wget http://fenecon.de/debian-test/goodweUpdate/goodweUpdateTool.sh -O /tmp/goodweUpdate.sh && bash /tmp/goodweUpdate.sh


# set -e

RED='\033[1;31m'
NORMAL='\033[0;39m'
GREEN='\033[1;32m'
echo "Prepare GoodWe Update Tool ..."

function downloadGoodweUpdate() {
echo "Dowload has to be done manually"
cp 
#wget http://fenecon.de/debian-test/goodweUpdate/goodwe20_30_test/goodweUpdate.jar -O /tmp/goodweUpdate.jar
#if [ $? -ne 0 ]; then return 8; fi
#wget http://fenecon.de/debian-test/goodweUpdate/goodwe20_30_test/arm.bin -O /tmp/$ARM_FW 
#if [ $? -ne 0 ]; then return 8; fi
#wget http://fenecon.de/debian-test/goodweUpdate/goodwe20_30_test/dsp.bin -O /tmp/$DSP_FW
#if [ $? -ne 0 ]; then return 8; fi
}


function goodWeUpdate() {
echo "Starte GoodWe Update"	
#java -jar /tmp/goodweUpdate.jar $PORT $BAUD_RATE /tmp/$ARM_FW /tmp/$DSP_FW
java -jar /home/fems/goodweUpdate.jar $PORT $BAUD_RATE /home/fems/$ARM_FW /home/fems/$DSP_FW
}

#sudo apt-get install rsync 
# rsync -r /ordner fems@192.168.100.1:/home/fems
#rsync Error Code 127 zurückbekommen, so kann man den Parameter --rsync-path verwenden (--rsync-path=/path/to/rsync)

# TODO: Add dynamic properties, stop & start if it should be used by Service
function handle_goodwe_30_update {
PORT=/dev/busUSB2
BAUD_RATE=9600
ARM_FW="arm.bin"
DSP_FW="dsp.bin"

# Herunterladen der Update-Datei
downloadGoodweUpdate
if [ $? -ne 0 ];
then
	echo -e "$RED \nGoodWe Update fehlgeschlagen! Bitte erneut versuchen! \nBitte prüfe ob das FEMS Zugang zum Internet hat $NORMAL "
	read -p "Bestätigen mit ENTER"
	return
fi

# Start GoodWe-Update
goodWeUpdate
if [ $? -ne 0 ];
then
	echo -e "$RED \nGoodWe Update fehlgeschlagen! Bitte erneut versuchen! \nBitte prüfe die Verkabelung zum Wechselrichter und den verwendeten Eingang am FEMS $NORMAL "
	read -p "Bestätigen mit ENTER"
	return
fi

echo ""
echo -e "$GREEN "
echo -e "#################################################"
echo -e "######## GoodWe Update abgeschlossen ############"
echo -e "################################################# $NORMAL "
read -p "Bestätigen mit ENTER"

}

function choose_setup {
	CHOICE=$(whiptail --menu "Select:" 20 60 12 \
		"Wechselrichter ist angeschlossen - jetzt update starten" "" \
		3>&1 1>&2 2>&3)

	case $CHOICE in
		"Wechselrichter ist angeschlossen - jetzt update starten")	handle_goodwe_30_update;;
	esac
}



while [ true ]; do

	# Start from calling parameter or show menu
	case "$1" in
		goodwe_30_update)				handle_goodwe_30_update;;
	*)									choose_setup;;
	esac

done

#TODO: could be adopted for every goodwe as long as update via montioring is not possible
#goodwe_10_update)					handle_goodwe_10_update;;


