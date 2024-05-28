#!/bin/bash
#
# Script to automatically copy a FEMS.

#																										  nr  password			 ODOO_USER_ID   ODOO_PASSWORD  LOCAL_FEMS(Optional)
# wget http://fenecon.de/debian-test/copyFems/copy-fems.sh -O /tmp/copy-fems.sh && bash /tmp/copy-fems.sh 3 Xe3q7RgcdIDjDFRAuitY 57096 cXTSfvWGNaZV2KjEJuAh
# wget http://fenecon.de/debian-test/copyFems/copy-fems.sh -O /tmp/copy-fems.sh && bash /tmp/copy-fems.sh 3154 mSC1b4fo5E9c6zLWwS2F 57096 cXTSfvWGNaZV2KjEJuAh

# Only for testing: java -jar /tmp/copy-fems.jar 57096 cXTSfvWGNaZV2KjEJuAh fems3
# wget http://fenecon.de/debian-test/copyFems/copy-fems.jar -O /tmp/copy-fems.jar && java -jar  /tmp/copy-fems.jar 57096 cXTSfvWGNaZV2KjEJuAh fems30061


# wget http://fenecon.de/debian-test/copyFems/copy-fems.sh -O /tmp/copy-fems.sh && bash /tmp/copy-fems.sh 5 jz2dwZ3VQvHynw5CxZvh 57096 cXTSfvWGNaZV2KjEJuAh
# wget http://fenecon.de/debian-test/copyFems/copy-fems.sh -O /tmp/copy-fems.sh && bash /tmp/copy-fems.sh 30407 AY2FOM7K3egUGFmBkfZz 57096 cXTSfvWGNaZV2KjEJuAh

#TODO: Separate Java Script that is checking if there is already a fems Nr. on the fems or not -> Currently it is not possible to run the script on a empty fems because we do not have a local-admin password

set -e

if [ "$EUID" -eq 0 ]; then
  echo "Start Copy Script"
else
  echo "Please execute as root"
  exit 1
fi

# Define environment variables
: "${ROOT_DIR:=/}" # Set / if ROOT_DIR has not been set
BASE_PATH="${ROOT_DIR}etc/openems.d"
BACKUP_PATH="${ROOT_DIR}usr/lib/openems/openems.d-backup-on-copy"
ETC_FEMS="${ROOT_DIR}etc/fems"
CONF_SCHEDULER="Scheduler/AllAlphabetically/d0ec9992-e625-4ee4-b252-c9d9d417d78c.config"
CONF_MODBUS0_RTU="Bridge/Modbus/Serial/c3c779fa-d763-4fb8-8546-6d9dbf7e5774.config"
CONF_MODBUS1_RTU="Bridge/Modbus/Serial/0f1dbe89-ddbd-4069-b0ba-fbece5a985d6.config"
CONF_MODBUS2_RTU="Bridge/Modbus/Serial/5109ef0f-3014-45e1-b62f-d788b5d75927.config"
CONF_MODBUS1_TCP_10_4_0_15="Bridge/Modbus/Tcp/129c3073-8bb6-4f55-911c-966092696cd5.config"
CONF_MODBUS1_TCP_10_4_0_10="Bridge/Modbus/Tcp/faf61c1f-9327-4edc-bf35-8280bb8fa94f.config"
CONF_MODBUS1_TCP_192_168_1_11="Bridge/Modbus/Tcp/39f1f42b-45af-4afd-a412-a27e5434e8f9.config"
CONF_CTRL_API_REST="Controller/Api/Rest/ReadWrite/bbcd5803-e577-4d88-b8eb-53c5d772687c.config"

FEMS=$1
PASSWORD=$2
ODOO_USER_ID=$3
ODOO_PASSWORD=$4

#
# Setup scheduler configuration for read-only systems (Mini, Pro Hybrid)
#
function setup_scheduler_readonly {
	echo "# Creating $CONF_SCHEDULER"
	mkdir -p "$(dirname $CONF_SCHEDULER)"
	cat >$CONF_SCHEDULER <<EOL
controllers.ids=[]
cycleTime=I"1000"
enabled=B"true"
id="schedulerTemp" 
service.factoryPid="Scheduler.AllAlphabetically"
service.pid="Scheduler.AllAlphabetically.d0ec9992-e625-4ee4-b252-c9d9d417d78c"
EOL
}

#
# Setup Controller Api.Rest
#
function setup_ctrl_api_rest {
	echo "# Creating $CONF_CTRL_API_REST"
	mkdir -p "$(dirname $CONF_CTRL_API_REST)"
	cat >$CONF_CTRL_API_REST <<EOL
apiTimeout="60"
enabled="true"
id="ctrlApiRestTemp"
port="8084"
service.factoryPid="Controller.Api.Rest.ReadWrite"
service.pid="Controller.Api.Rest.ReadWrite.bbcd5803-e577-4d88-b8eb-53c5d772687c"
EOL
}


#
# Setup configuration that is valid for every system
#
function setup_generic {
	if [ -d $BASE_PATH ]; then
	
        echo "# Backup openems.d folder to /usr/lib/openems/openems.d-backup-on-copy"
		mkdir -p /usr/lib/openems
        cp -r $BASE_PATH $BACKUP_PATH
        #cp -r /etc/openems.d /usr/lib/openems/openems.d-backup-on-copy
	
		echo "# Deleting $BASE_PATH"
		find $BASE_PATH ! -wholename "$BASE_PATH" ! -name 'config.json' -exec rm -rf {} +
	fi
	echo "# Creating $BASE_PATH"
	mkdir -p $BASE_PATH
	cd $BASE_PATH


	echo "Add Rest Api"
	setup_ctrl_api_rest
}


function setup_basis {

	setup_generic
	setup_scheduler_readonly
}

function installTmpFems {
echo 
echo "##############################################################################################################################"
echo "FEMS ist leer! Aktuell ist es nur möglich ein fems zu überspielen, wenn das aktuell bereits eine Basis FEMS-Installation hat."
echo ""
echo "1. Wenn FEMS$FEMS online ist: "
echo "  - Installiere ein fems in einem vorher definiertem Bereich (3600 - 3610)"
echo ""
echo "2. Wenn FEMS$FEMS offline ist: "
echo "  - Entweder 1. oder zu kopierendes fems"
echo ""
echo "ACHTUNG! Wird manuell die zu kopierende Nummer über Fiona installiert, wird die bestehende Odoo Konfiguration überschrieben!!!"
echo "##############################################################################################################################"
echo ""
echo "Folgendes installiert nur die Basics, ohne dass sich das fems mit dem Backend verbindet"
echo ""
read -p "Meldung gelesen? Press Enter to continue ..."
echo ""
read -p "Enter FEMS-Nr (without fems): " tmpFemsNr
read -p "Enter password: " tmpPassword
read -p "Enter ApiKey: " tmpApikey
echo ""
echo "FEMS: fems$tmpFemsNr"
echo "Passwort: $tmpPassword"
echo "ApiKey: $tmpApikey"
echo ""
echo ""
read -p "Check inputs! Press Enter to continue ..."
		
echo "# Installing FEMS package"
wget http://fenecon.de/debian-test/update-fems.sh -O /tmp/update-fems.sh && bash /tmp/update-fems.sh

femsName=fems${tmpFemsNr}
echo "# Updating /etc/hostname"
echo femsName > /etc/hostname
cat /etc/hostname

echo "# Updating /etc/hosts"
sed "s/\(127.0.1.1.*\)beaglebone.*/\1$femsName/" /etc/hosts --in-place
/bin/sed "s/127.0.0.1.*/127.0.0.1 localhost $femsName/" /etc/hosts --in-place
/bin/sed "s/127.0.1.1.*/127.0.1.1 $femsName/" /etc/hosts --in-place
cat /etc/hosts

echo "# Updating /etc/fems"
echo "apikey=${tmpApikey}" | tee /etc/fems
cat /etc/fems

echo "# Updating password"
echo "fems:${tmpPassword}" | /usr/sbin/chpasswd

rm -fv /etc/ssh/ssh_host_* && 
dpkg-reconfigure openssh-server && 

clear
echo "# Installed temporary fems"

reboot
}

echo "#"
echo "# FEMS: fems$FEMS"
echo "# Passwort: $PASSWORD"
echo "#"
echo "#"
read -p "Check inputs! Press Enter to continue ..."

echo "Add modified openems configuration"
setup_basis

if [ ! -f $ETC_FEMS ]; then	
	installTmpFems
fi


# Im Java - Probiere mehrmals zu lesen - evtl. dauert neustart länger und rest ist noch nicht aktiv
systemctl restart openems --no-block;


wget http://fenecon.de/debian-test/copyFems/progress-bar.sh -O /tmp/progress-bar.sh 

echo ""
echo "Make sure that everything is running. This will take some time ..."
bash /tmp/progress-bar.sh 2


# AFFWHELLKD7T6YQF
wget http://fenecon.de/debian-test/copyFems/copy-fems.jar -O /tmp/copy-fems.jar




if [ -z "$5" ]
  then
	echo "No argument for local hostname given"	
	java -jar  /tmp/copy-fems.jar $ODOO_USER_ID $ODOO_PASSWORD fems$FEMS
else
	#TODO Change to argument
	echo "Local admin passed"	
	java -jar  /tmp/copy-fems.jar $ODOO_USER_ID $ODOO_PASSWORD fems$FEMS $5
fi
#java -jar  /tmp/copy-fems.jar $ODOO_USER_ID $ODOO_PASSWORD fems$FEMS $LOCAL_PASSWORD
#java -jar  /tmp/copy-fems.jar 57096 cXTSfvWGNaZV2KjEJuAh fems3 AQNRI5TCSEI2UNB6

echo "${FEMS}: Setting up FEMS"

#TODO: Argument to avoid update (For snapshot versions)
wget http://fenecon.de/debian-test/update-fems.sh -O /tmp/update-fems.sh && bash /tmp/update-fems.sh
ln -sf /run/systemd/resolve/resolv.conf /etc/resolv.conf

# Updating FEMS Number, to be able to install components with the local admin password of the new fems nr
echo "# ${FEMS}: Updating /etc/hostname"
echo fems${FEMS} > /etc/hostname
cat /etc/hostname

echo "# ${FEMS}: Updating /etc/hosts"
/bin/sed "s/127.0.0.1.*/127.0.0.1 localhost fems${FEMS}/" /etc/hosts --in-place
/bin/sed "s/127.0.1.1.*/127.0.1.1 fems${FEMS}/" /etc/hosts --in-place
cat /etc/hosts


echo "fems:${PASSWORD}" | /usr/sbin/chpasswd
rm -fv /etc/ssh/ssh_host_* && 
dpkg-reconfigure openssh-server


# TODO: Fallback wenns nicht funktioniert
# Snapshot mit Detailierter beschreibung aufm server?


echo ""
echo ""
echo "# #################################################"
echo "# ################# FINISHED ######################"
echo "# #################################################"
echo ""
echo "# Please compare the old copy with the new one after the reboot"
echo ""
echo "-> Old Config: /usr/lib/openems/oldconfig.txt"
echo "-> New Config: https://erp.fenecon.de/web#action=1062&model=fems.device&view_type=list&cids=1&menu_id=733"
echo ""
echo "# If your connected via ssh -> FEMS Number changed - Please login to with new fems number"
echo ""
reboot



