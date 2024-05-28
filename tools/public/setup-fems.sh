#!/bin/bash
#
# Script to automatically setup a FEMS.

set -e

# Define environment variables
: "${ROOT_DIR:=/}" # Set / if ROOT_DIR has not been set
BASE_PATH="${ROOT_DIR}etc/openems.d"
ETC_FEMS="${ROOT_DIR}etc/fems"
CONF_SCHEDULER="Scheduler/AllAlphabetically/d0ec9992-e625-4ee4-b252-c9d9d417d78c.config"
CONF_MODBUS0_RTU="Bridge/Modbus/Serial/c3c779fa-d763-4fb8-8546-6d9dbf7e5774.config"
CONF_MODBUS1_RTU="Bridge/Modbus/Serial/0f1dbe89-ddbd-4069-b0ba-fbece5a985d6.config"
CONF_MODBUS2_RTU="Bridge/Modbus/Serial/5109ef0f-3014-45e1-b62f-d788b5d75927.config"
CONF_MODBUS1_TCP_10_4_0_15="Bridge/Modbus/Tcp/129c3073-8bb6-4f55-911c-966092696cd5.config"
CONF_MODBUS1_TCP_10_4_0_10="Bridge/Modbus/Tcp/faf61c1f-9327-4edc-bf35-8280bb8fa94f.config"
CONF_MODBUS1_TCP_192_168_1_11="Bridge/Modbus/Tcp/39f1f42b-45af-4afd-a412-a27e5434e8f9.config"

# Pro 9-12
CONF_ESS0_PRO912="Fenecon/Pro/Ess/eb56f839-f4f0-4845-a3ec-333b75f2b578.config"
CONF_METER1_PRO912="Fenecon/Pro/PvMeter/8ef26eeb-10e7-4325-b7d8-f2b83cfa04fa.config"

# SOCOMEC Grid Meter
CONF_METER0_SOCOMEC="Meter/Socomec/Threephase/63004a9d-b91a-4fbc-ad1f-bb550dab9d5a.config"
CONF_METER0_SOCOMEC_MODBUS2="Meter/Socomec/Threephase/63004a9d-b91a-4fbc-ad1f-bb550dab9d5a.config"
CONF_METER1_SOCOMEC_MODBUS2="Meter/Socomec/Threephase/81cdf0a3-6fe8-4c44-aaf6-237d1392ade8.config"

# Mini 3-3
CONF_ESS0_MINI="Fenecon/Mini/Ess/abe351b3-34dc-4184-8c07-26a7196b4838.config"
CONF_METER0_MINI="Fenecon/Mini/GridMeter/97d43d79-cf75-42cf-8141-d0a7c2c3fcba.config"
CONF_METER1_MINI="Fenecon/Mini/PvMeter/7e0f4e99-0083-4ae2-aaa1-afc3b51f3b6b.config"

# Pro Hybrid 10-Series (KACO)
CONF_CORE_PROHYBRID10="Kaco/BlueplanetHybrid10/Core/b11f6d44-631d-49a7-a9eb-fd063aa1ed3e.config"
CONF_ESS0_PROHYBRID10="Kaco/BlueplanetHybrid10/Ess/0bb53c70-83ff-40ba-b351-1c4df781ab4f.config"
CONF_METER0_PROHYBRID10="Kaco/BlueplanetHybrid10/GridMeter/008bcc03-a824-4cad-9fcd-993bbbb6f4ad.config"
CONF_CHARGER0_PROHYBRID10="Kaco/BlueplanetHybrid10/Charger/30b51193-e256-42b9-a743-b1c043c713fa.config"

CONF_ESS_POWER="Ess/Power.config"

CONF_CTRL_DEBUG_LOG="Controller/Debug/Log/28233476-3bb6-4773-b8f5-c2a7a47b85a2.config"
CONF_CTRL_API_BACKEND="Controller/Api/Backend/da205ba7-b0d7-4758-bd9c-78381badbbf2.config"
CONF_CTRL_API_REST="Controller/Api/Rest/bbcd5803-e577-4d88-b8eb-53c5d772687c.config"
CONF_CTRL_API_MODBUS_TCP="Controller/Api/ModbusTcp/ReadOnly/955cac6b-1f87-4f4e-aa8c-15b9520a8f10.config"
CONF_CTRL_API_WEBSOCKET="Controller/Api/Websocket/9dc51b52-feb6-4f5d-a3b3-5ee6997b20c9.config"
CONF_CTRL_LIMIT_TOTAL_DISCHARGE="Controller/Ess/LimitTotalDischarge/9621549c-ece7-4e70-9f48-c685229be4d5.config"
CONF_CTRL_BALANCING_SYMMETRIC="Controller/Symmetric/Balancing/4e1667dc-ce19-4870-bdb6-475d639d832a.config"

# RRD4j
CONF_TIMEDATA_RRD4J="Timedata/Rrd4j/89268e1c-3704-4329-b9ed-49949807d2fb.config"

# DESS
CONF_CHARGER0_DESS="Fenecon/Dess/Charger1/0d3c54c2-50fe-43e9-bfcf-f7e29420f515.config"
CONF_CHARGER1_DESS="Fenecon/Dess/Charger2/d5bd8868-944b-45da-9a73-7f8e8931ed50.config"
CONF_ESS0_DESS="Fenecon/Dess/Ess/bd252265-3f6e-4609-b362-61f26e87b6c4.config"
CONF_METER0_DESS="Fenecon/Dess/GridMeter/369dee25-e723-4699-8b47-40a4e5a80791.config"
CONF_METER1_DESS="Fenecon/Dess/PvMeter/56b6c914-6ce8-4219-a9d8-84f54b4ebea4.config"

# Commercial 40
CONF_ESS0_COMMERCIAL_40="Ess/Fenecon/Commercial40/2032b36c-f232-4463-9458-2fd1a9f19f33.config"
CONF_CHARGER0_COMMERCIAL_40="EssDcCharger/Fenecon/Commercial40/e5912787-a377-49f5-af43-2bfe0ccf9ad7.config"
CONF_CTRL_COMMERCIAL_40_SURPLUS="Controller/Ess/FeneconCommercial40SurplusFeedIn/20b5c675-baa6-46e7-9be2-f13b5609d25c.config"
CONF_BMS0_SOLTARO_SINGLE_RACK="Bms/Soltaro/SingleRack/VersionB/09f38574-b9d7-48ba-a993-110b7f5810a3.config"

# Commercial 50 + 30
CONF_BMS0_SOLTARO_SINGLE_RACK_VERSION_C="Bms/Soltaro/SingleRack/VersionC/5b046fa5-a089-4239-a08a-7cc7927fb56e.config"
CONF_ESS0_KACO_BLUEPLANET_GRIDSAVE_50="Ess/Kaco/BlueplanetGridsave50/22a16c3b-0147-41a4-8753-98de6860b339.config"
CONF_BATTERY_INVERTER0_KACO_BLUEPLANET_GRIDSAVE="Battery-Inverter/Kaco/BlueplanetGridsave/a42c2cec-2850-47f7-a1ae-fc4decdbb2c0.config"
CONF_BATTERY_INVERTER_SINEXCEL="Battery-Inverter/Sinexcel/c543b730-87d6-4c18-9869-6976aec436c2.config"
CONF_ESS_GENERIC_MANAGED_SYMMETRIC="Ess/Generic/ManagedSymmetric/4b518bf7-5989-4c73-a0cb-ff6855fd4992.config"

# Commercial 30 Offgrid
CONF_RELAY_WAGO_MODBUS3_TCP_192_168_1_50="Bridge/Modbus/Tcp/4238ca19-8151-47fb-a9f6-78d213a697b2.config"
CONF_RELAY_WAGO_IO0="IO/WAGO/1c6498cd-1bbd-427e-a9a0-6fabc67a88d2.config"
CONF_OFFGRID_SWITCH="Io/Off/Grid/Switch/65163c6a-0833-4c3f-8061-cdea678a0714.config"
CONF_ESS_GENERIC_OFFGRID="Ess/Generic/OffGrid/37d1bdb9-a33d-422e-a05b-b196af282f7f.config"
CONF_CTRL_EMERGENCY_CAPACITY_RESERVE="Controller/Ess/EmergencyCapacityReserve/e0ba42f5-13e5-4684-af42-6e91c4361d15.config"

# FENECON HOME
CONF_BATTERY_FENECON_HOME="Battery/Fenecon/Home/6b2b91c4-b68d-4c76-b6b4-f811a7999e29.config"
CONF_BATTERY_INVERTER0_GOODWE="GoodWe/BatteryInverter/9e6e0862-2d28-4747-89d2-963c1cef9861.config"
CONF_CTRL_ESS_SURPULUS_FEED_TO_GRID0="Controller/Ess/Hybrid/Surplus-Feed-To-Grid/cd963733-b33e-47dd-a77d-382532783519.config"
CONF_RELAY_KMTRONIK_4PORT_IO0="IO/KMtronic/4Port/bd49fc20-b806-4b38-a0c9-c559470c4f54.config"

# Commercial GEN_2
CONF_BATTERY_SOLTARO_GEN_2="Battery/Fenecon/Commercial/8b16b276-963b-4cc0-96a1-bc7b68058222.config"
CONF_RELAY_KMTRONIK_IO0="IO/KMtronic/3e636f4f-5a1e-4bdd-8b2d-160e08c55f34.config"
CONF_MODBUS2_RTU_2="Bridge/Modbus/Serial/e4523e79-7eed-4468-9ac4-53af4e7ffe52.config"


# GoodWe ET
CONF_ESS0_GOODWE_ET="GoodWe/Ess/9784f354-1886-4c12-9b17-596d106f95e2.config"
CONF_CHARGER0_GOODWE_ET="GoodWe/Charger-PV1/af91ae04-dc4d-45b3-87de-23ab03a4b57e.config"
CONF_CHARGER1_GOODWE_ET="GoodWe/Charger-PV2/98ec1eae-f286-445e-8dbe-7ebe9e6c1ac6.config"
CONF_METER0_GOODWE_ET="GoodWe/Grid-Meter/af707ccf-0e4c-41f2-9cac-6dfd8295df33.config"

# Kostal Piko
CONF_KOSTAL_PIKO_CHARGER="Kostal/Piko/Charger/291f3b7b-c12c-40c3-8927-610b9c725c44.config"
CONF_KOSTAL_PIKO_CORE="Kostal/Piko/Core/14f7de38-e86e-489c-acfb-2ed15476c530.config"
CONF_KOSTAL_PIKO_ESS="Kostal/Piko/Ess/28f7ea70-95ec-42d1-987a-7b4784bdc7d0.config"
CONF_KOSTAL_PIKO_GRIDMETER="Kostal/Piko/GridMeter/24d87e3f-904b-4473-83cf-e08046833f57.config"

# Initial checks
if [ ! -e $ETC_FEMS ]; then
	echo "File is missing: $ETC_FEMS"
	exit 1
fi;
APIKEY=$(grep 'apikey=' $ETC_FEMS | cut -d'=' -f2)

#
# Setup configuration that is valid for every system
#
function setup_generic {
	if [ -d $BASE_PATH ]; then
		echo "# Deleting $BASE_PATH"
		find $BASE_PATH ! -wholename "$BASE_PATH" ! -name 'config.json' -exec rm -rf {} +
	fi
	echo "# Creating $BASE_PATH"
	mkdir -p $BASE_PATH
	cd $BASE_PATH

	setup_ctrl_debug_log
	setup_ctrl_api_backend
	setup_ctrl_api_modbus_tcp
	#setup_ctrl_api_rest
	setup_ctrl_api_websocket
	setup_timedata_rrd4j
}

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
id="scheduler0"
service.factoryPid="Scheduler.AllAlphabetically"
service.pid="Scheduler.AllAlphabetically.d0ec9992-e625-4ee4-b252-c9d9d417d78c"
EOL
}

#
# Setup scheduler configuration for controlled systems (Pro 9-12, Commercial 40)
#
function setup_scheduler_controlled {
	echo "# Creating $CONF_SCHEDULER"
	mkdir -p "$(dirname $CONF_SCHEDULER)"
	cat >$CONF_SCHEDULER <<EOL
controllers.ids=[ \
  "ctrlLimitTotalDischarge0", \
  "ctrlBalancing0", \
  ]
cycleTime=I"1000"
enabled=B"true"
id="scheduler0"
service.factoryPid="Scheduler.AllAlphabetically"
service.pid="Scheduler.AllAlphabetically.d0ec9992-e625-4ee4-b252-c9d9d417d78c"
EOL
}

#
# Setup scheduler configuration for controlled systems with only balancing
#
function setup_scheduler_controlled_balancing {
	echo "# Creating $CONF_SCHEDULER"
	mkdir -p "$(dirname $CONF_SCHEDULER)"
	cat >$CONF_SCHEDULER <<EOL
controllers.ids=[ \
  "ctrlBalancing0", \
  ]
cycleTime=I"1000"
enabled=B"true"
id="scheduler0"
service.factoryPid="Scheduler.AllAlphabetically"
service.pid="Scheduler.AllAlphabetically.d0ec9992-e625-4ee4-b252-c9d9d417d78c"
EOL
}

#
# Setup scheduler configuration for controlled systems (commercial_30 emergency)
#
function setup_scheduler_controlled_emergency {
	echo "# Creating $CONF_SCHEDULER"
	mkdir -p "$(dirname $CONF_SCHEDULER)"
	cat >$CONF_SCHEDULER <<EOL
controllers.ids=[ \
  "ctrlEmergencyCapacityReserve0", \
  "ctrlBalancing0", \
  ]
cycleTime=I"1000"
enabled=B"true"
id="scheduler0"
service.factoryPid="Scheduler.AllAlphabetically"
service.pid="Scheduler.AllAlphabetically.d0ec9992-e625-4ee4-b252-c9d9d417d78c"
EOL
}

#
# Setup scheduler configuration for controlled HYBRID systems (HOME, [PRO_HYBRID 10, PRO_HYBRID GW not 'read-only'])
#
function setup_scheduler_hybrid_controlled {
	echo "# Creating $CONF_SCHEDULER"
	mkdir -p "$(dirname $CONF_SCHEDULER)"
	cat >$CONF_SCHEDULER <<EOL
controllers.ids=[ \
  "ctrlEssSurplusFeedToGrid0", \
  "ctrlBalancing0", \
  ]
cycleTime=I"1000"
enabled=B"true"
id="scheduler0"
service.factoryPid="Scheduler.AllAlphabetically"
service.pid="Scheduler.AllAlphabetically.d0ec9992-e625-4ee4-b252-c9d9d417d78c"
EOL
}

#
# Setup modbus0 RTU
#
function setup_modbus0_rtu {
	echo "# Creating $CONF_MODBUS0_RTU"
	mkdir -p "$(dirname $CONF_MODBUS0_RTU)"
	cat >$CONF_MODBUS0_RTU <<EOL
baudRate=I"9600"
databits=I"8"
enabled=B"true"
id="modbus0"
parity="NONE"
portName="/dev/ttyUSB0"
service.factoryPid="Bridge.Modbus.Serial"
service.pid="Bridge.Modbus.Serial.c3c779fa-d763-4fb8-8546-6d9dbf7e5774"
stopbits="ONE"
EOL
}

#
# Setup modbus0 RTU with docking port
#
function setup_modbus0_rtu_docking_busUSB1 {
	echo "# Creating $CONF_MODBUS0_RTU"
	mkdir -p "$(dirname $CONF_MODBUS0_RTU)"
	cat >$CONF_MODBUS0_RTU <<EOL
baudRate=I"9600"
databits=I"8"
enabled=B"true"
id="modbus0"
parity="NONE"
portName="/dev/busUSB1"
service.factoryPid="Bridge.Modbus.Serial"
service.pid="Bridge.Modbus.Serial.c3c779fa-d763-4fb8-8546-6d9dbf7e5774"
stopbits="ONE"
EOL
}

#
# Setup modbus0 RTU with /dev/bus0 and baudRate 57600 
#
function setup_modbus0_rtu_bus0_57600 {
	echo "# Creating $CONF_MODBUS0_RTU"
	mkdir -p "$(dirname $CONF_MODBUS0_RTU)"
	cat >$CONF_MODBUS0_RTU <<EOL
alias="Kommunikation\\ mit\\ der\\ Batterie"
baudRate=I"57600"
databits=I"8"
enabled=B"true"
id="modbus0"
invalidateElementsAfterReadErrors=I"3"
logVerbosity="NONE"
parity="NONE"
portName="/dev/bus0"
service.factoryPid="Bridge.Modbus.Serial"
service.pid="Bridge.Modbus.Serial.c3c779fa-d763-4fb8-8546-6d9dbf7e5774"
stopbits="ONE"
EOL
}

#
# Setup modbus0 RTU with /dev/bus0 and baudRate 57600 with docking port
#
function setup_modbus0_rtu_bus0_57600_docking_busUSB1 {
	echo "# Creating $CONF_MODBUS0_RTU"
	mkdir -p "$(dirname $CONF_MODBUS0_RTU)"
	cat >$CONF_MODBUS0_RTU <<EOL
alias="Kommunikation\\ mit\\ der\\ Batterie"
baudRate=I"57600"
databits=I"8"
enabled=B"true"
id="modbus0"
invalidateElementsAfterReadErrors=I"3"
logVerbosity="NONE"
parity="NONE"
portName="/dev/busUSB1"
service.factoryPid="Bridge.Modbus.Serial"
service.pid="Bridge.Modbus.Serial.c3c779fa-d763-4fb8-8546-6d9dbf7e5774"
stopbits="ONE"
EOL
}

#
# Setup modbus0 RTU for techbase with port AMA0
#
function setup_modbus0_rtu_techbase_AMA0 {
	echo "# Creating $CONF_MODBUS0_RTU"
	mkdir -p "$(dirname $CONF_MODBUS0_RTU)"
	cat >$CONF_MODBUS0_RTU <<EOL
alias="Kommunikation\\ mit\\ der\\ Batterie"
baudRate=I"9600"
databits=I"8"
enabled=B"true"
id="modbus0"
invalidateElementsAfterReadErrors=I"3"
logVerbosity="NONE"
parity="NONE"
portName="/dev/ttyAMA0"
service.factoryPid="Bridge.Modbus.Serial"
service.pid="Bridge.Modbus.Serial.c3c779fa-d763-4fb8-8546-6d9dbf7e5774"
stopbits="ONE"
EOL
}

#
# Setup modbus2 RTU for techbase with port SC0
#
function setup_modbus2_rtu_techbase_SC0 {
	echo "# Creating $CONF_MODBUS2_RTU_2"
	mkdir -p "$(dirname $CONF_MODBUS2_RTU_2)"
	cat >$CONF_MODBUS2_RTU_2 <<EOL
alias="Kommunikation\\ mit\\ den\\ Zählern"
id="modbus2"
invalidateElementsAfterReadErrors=I"3"
portName="/dev/ttySC0"
service.factoryPid="Bridge.Modbus.Serial"
service.pid="Bridge.Modbus.Serial.e4523e79-7eed-4468-9ac4-53af4e7ffe52"
EOL
}

#
# Setup modbus2 RTU with /dev/bus1 and baudRate 9600 
#
function setup_modbus2_rtu_bus1_9600 {
	echo "# Creating $CONF_MODBUS2_RTU"
	mkdir -p "$(dirname $CONF_MODBUS2_RTU)"
	cat >$CONF_MODBUS2_RTU <<EOL
alias="Kommunikation\\ mit\\ den\\ Zählern"
baudRate=I"9600"
databits=I"8"
enabled=B"true"
id="modbus2"
invalidateElementsAfterReadErrors=I"3"
logVerbosity="NONE"
parity="NONE"
portName="/dev/bus1"
service.factoryPid="Bridge.Modbus.Serial"
service.pid="Bridge.Modbus.Serial.5109ef0f-3014-45e1-b62f-d788b5d75927"
stopbits="ONE"
EOL
}

#
# Setup modbus2 RTU with /dev/bus1 and baudRate 9600 with docking
#
function setup_modbus2_rtu_bus1_9600_docking_busUSB2 {
	echo "# Creating $CONF_MODBUS2_RTU"
	mkdir -p "$(dirname $CONF_MODBUS2_RTU)"
	cat >$CONF_MODBUS2_RTU <<EOL
alias="Kommunikation\\ mit\\ den\\ Zählern"
baudRate=I"9600"
databits=I"8"
enabled=B"true"
id="modbus2"
invalidateElementsAfterReadErrors=I"3"
logVerbosity="NONE"
parity="NONE"
portName="/dev/busUSB2"
service.factoryPid="Bridge.Modbus.Serial"
service.pid="Bridge.Modbus.Serial.5109ef0f-3014-45e1-b62f-d788b5d75927"
stopbits="ONE"
EOL
}

#
# Setup modbus0 RTU with /dev/busUSB1 and baudRate 19200 
#
function setup_modbus0_rtu_usb_docking_busUSB1_baud_19200 {
	echo "# Creating $CONF_MODBUS0_RTU"
	mkdir -p "$(dirname $CONF_MODBUS0_RTU)"
	cat >$CONF_MODBUS0_RTU <<EOL
alias="Kommunikation\\ mit\\ der\\ Batterie"
baudRate=I"19200"
databits=I"8"
enabled=B"true"
id="modbus0"
invalidateElementsAfterReadErrors=I"3"
logVerbosity="NONE"
parity="NONE"
portName="/dev/busUSB1"
service.factoryPid="Bridge.Modbus.Serial"
service.pid="Bridge.Modbus.Serial.c3c779fa-d763-4fb8-8546-6d9dbf7e5774"
stopbits="ONE"
EOL
}

#
# Setup modbus1 RTU with /dev/busUSB2 and baudRate 9600 
#
function setup_modbus1_rtu_usb_docking_busUSB2_baud_9600 {
	echo "# Creating $CONF_MODBUS1_RTU"
	mkdir -p "$(dirname $CONF_MODBUS1_RTU)"
	cat >$CONF_MODBUS1_RTU <<EOL
alias="Kommunikation\\ mit\\ dem\\ Batterie\\ WR"
baudRate=I"9600"
databits=I"8"
enabled=B"true"
id="modbus1"
invalidateElementsAfterReadErrors=I"3"
logVerbosity="NONE"
parity="NONE"
portName="/dev/busUSB2"
service.factoryPid="Bridge.Modbus.Serial"
service.pid="Bridge.Modbus.Serial.0f1dbe89-ddbd-4069-b0ba-fbece5a985d6"
stopbits="ONE"
EOL
}

#
# Setup ess0 Pro 9-12
#
function setup_ess0_pro912 {
	echo "# Creating $CONF_ESS0_PRO912"
	mkdir -p "$(dirname $CONF_ESS0_PRO912)"
	cat >$CONF_ESS0_PRO912 <<EOL
Modbus.target="(&(enabled\=true)(!(service.pid\=Fenecon.Pro.Ess.eb56f839-f4f0-4845-a3ec-333b75f2b578))(|(id\=modbus0)))"
enabled=B"true"
id="ess0"
modbus.id="modbus0"
service.factoryPid="Fenecon.Pro.Ess"
service.pid="Fenecon.Pro.Ess.eb56f839-f4f0-4845-a3ec-333b75f2b578"
EOL
}

#
# Setup ess0 Mini
#
function setup_ess0_mini {
	echo "# Creating $CONF_ESS0_MINI"
	mkdir -p "$(dirname $CONF_ESS0_MINI)"
	cat >$CONF_ESS0_MINI <<EOL
Modbus.target="(&(enabled\=true)(!(service.pid\=Fenecon.Mini.Ess.abe351b3-34dc-4184-8c07-26a7196b4838))(|(id\=modbus0)))"
enabled=B"true"
id="ess0"
modbus.id="modbus0"
phase="L1"
service.factoryPid="Fenecon.Mini.Ess"
service.pid="Fenecon.Mini.Ess.abe351b3-34dc-4184-8c07-26a7196b4838"
EOL
}

#
# Setup meter0 Mini Grid-Meter
#
function setup_meter0_mini {
	echo "# Creating $CONF_METER0_MINI"
	mkdir -p "$(dirname $CONF_METER0_MINI)"
	cat >$CONF_METER0_MINI <<EOL
Modbus.target="(&(enabled\=true)(!(service.pid\=Fenecon.Mini.GridMeter.97d43d79-cf75-42cf-8141-d0a7c2c3fcba))(|(id\=modbus0)))"
enabled=B"true"
id="meter0"
modbus.id="modbus0"
service.factoryPid="Fenecon.Mini.GridMeter"
service.pid="Fenecon.Mini.GridMeter.97d43d79-cf75-42cf-8141-d0a7c2c3fcba"
EOL
}

#
# Setup meter0 Mini Grid-Meter
#
function setup_meter1_mini {
	echo "# Creating $CONF_METER1_MINI"
	mkdir -p "$(dirname $CONF_METER1_MINI)"
	cat >$CONF_METER1_MINI <<EOL
Modbus.target="(&(enabled\=true)(!(service.pid\=Fenecon.Mini.PvMeter.7e0f4e99-0083-4ae2-aaa1-afc3b51f3b6b))(|(id\=modbus0)))"
enabled=B"true"
id="meter1"
modbus.id="modbus0"
service.factoryPid="Fenecon.Mini.PvMeter"
service.pid="Fenecon.Mini.PvMeter.7e0f4e99-0083-4ae2-aaa1-afc3b51f3b6b"
EOL
}

#
# Setup Ess.Power
#
function setup_ess_power_symmetric_mode {
	echo "# Creating $CONF_ESS_POWER"
	mkdir -p "$(dirname $CONF_ESS_POWER)"
	cat >$CONF_ESS_POWER <<EOL
debugMode=B"false"
service.pid="Ess.Power"
strategy="OPTIMIZE_BY_MOVING_TOWARDS_TARGET"
symmetricMode=B"true"
EOL
}

#
# Setup meter1 Pro 9-12 PV-Meter
#
function setup_meter1_pro912 {
	echo "# Creating $CONF_METER1_PRO912"
	mkdir -p "$(dirname $CONF_METER1_PRO912)"
	cat >$CONF_METER1_PRO912 <<EOL
Modbus.target="(&(enabled\=true)(!(service.pid\=Fenecon.Pro.PvMeter.8ef26eeb-10e7-4325-b7d8-f2b83cfa04fa))(|(id\=modbus0)))"
enabled=B"true"
id="meter1"
modbus.id="modbus0"
service.factoryPid="Fenecon.Pro.PvMeter"
service.pid="Fenecon.Pro.PvMeter.8ef26eeb-10e7-4325-b7d8-f2b83cfa04fa"
EOL
}

#
# Setup meter0 Socomec ID5 Grid
#
function setup_meter0_socomec {
	echo "# Creating $CONF_METER0_SOCOMEC"
	mkdir -p "$(dirname $CONF_METER0_SOCOMEC)"
	cat >$CONF_METER0_SOCOMEC <<EOL
Modbus.target="(&(enabled\=true)(!(service.pid\=Meter.Socomec.Threephase.63004a9d-b91a-4fbc-ad1f-bb550dab9d5a))(|(id\=modbus0)))"
alias="Netz"
enabled=B"true"
id="meter0"
invert=B"false"
modbus.id="modbus0"
modbusUnitId=I"5"
service.factoryPid="Meter.Socomec.Threephase"
service.pid="Meter.Socomec.Threephase.63004a9d-b91a-4fbc-ad1f-bb550dab9d5a"
type="GRID"
EOL
}

#
# Setup meter0 Socomec - ID5 - Grid - modbus2 
#
function setup_meter0_socomec_modbus2 {
	echo "# Creating $CONF_METER0_SOCOMEC_MODBUS2"
	mkdir -p "$(dirname $CONF_METER0_SOCOMEC_MODBUS2)"
	cat >$CONF_METER0_SOCOMEC <<EOL	
Modbus.target="(&(enabled\=true)(!(service.pid\=Meter.Socomec.Threephase.63004a9d-b91a-4fbc-ad1f-bb550dab9d5a))(|(id\=modbus2)))"
alias="Netz"
enabled=B"true"
id="meter0"
invert=B"false"
modbus.id="modbus2"
modbusUnitId=I"5"
service.factoryPid="Meter.Socomec.Threephase"
service.pid="Meter.Socomec.Threephase.63004a9d-b91a-4fbc-ad1f-bb550dab9d5a"
type="GRID"
EOL
}

#
# Setup meter1 Socomec - ID6 - Produktion - modbus2 
#
function setup_meter1_socomec_modbus2 {
	echo "# Creating $CONF_METER1_SOCOMEC_MODBUS2"
	mkdir -p "$(dirname $CONF_METER1_SOCOMEC_MODBUS2)"
	cat >$CONF_METER1_SOCOMEC_MODBUS2 <<EOL	
Modbus.target="(&(enabled\=true)(!(service.pid\=Meter.Socomec.Threephase.81cdf0a3-6fe8-4c44-aaf6-237d1392ade8))(|(id\=modbus2)))"
alias="Produktion"
enabled=B"true"
id="meter1"
invert=B"false"
modbus.id="modbus2"
modbusUnitId="6"
service.factoryPid="Meter.Socomec.Threephase"
service.pid="Meter.Socomec.Threephase.81cdf0a3-6fe8-4c44-aaf6-237d1392ade8"
type="PRODUCTION"
EOL
}

#
# Setup Controller Debug.Log
#
function setup_ctrl_debug_log {
	echo "# Creating $CONF_CTRL_DEBUG_LOG"
	mkdir -p "$(dirname $CONF_CTRL_DEBUG_LOG)"
	cat >$CONF_CTRL_DEBUG_LOG <<EOL
enabled=B"true"
id="ctrlDebugLog0"
service.factoryPid="Controller.Debug.Log"
service.pid="Controller.Debug.Log.28233476-3bb6-4773-b8f5-c2a7a47b85a2"
EOL
}

#
# Setup Controller Api.Backend
#
function setup_ctrl_api_backend {
	echo "# Creating $CONF_CTRL_API_BACKEND"
	mkdir -p "$(dirname $CONF_CTRL_API_BACKEND)"
	cat >$CONF_CTRL_API_BACKEND <<EOL
apiTimeout=I"60"
apikey="${APIKEY}"
cycleTime=I"10000"
debug=B"false"
enabled=B"true"
id="ctrlBackend0"
proxyAddress=""
proxyPort=I"0"
proxyType="HTTP"
service.factoryPid="Controller.Api.Backend"
service.pid="Controller.Api.Backend.da205ba7-b0d7-4758-bd9c-78381badbbf2"
uri="wss://www1.fenecon.de:443/openems-backend2"
EOL
}

#
# Setup Controller Api.Rest
#
#function setup_ctrl_api_rest {
#	echo "# Creating $CONF_CTRL_API_REST"
#	mkdir -p "$(dirname $CONF_CTRL_API_REST)"
#	cat >$CONF_CTRL_API_REST <<EOL
#apiTimeout="60"
#enabled="true"
#id="ctrlApiRest0"
#port="8084"
#service.factoryPid="Controller.Api.Rest"
#service.pid="Controller.Api.Rest.bbcd5803-e577-4d88-b8eb-53c5d772687c"
#EOL
#}

#
# Setup Controller Api.Rest
#
function setup_ctrl_api_modbus_tcp {
	echo "# Creating $CONF_CTRL_API_MODBUS_TCP"
	mkdir -p "$(dirname $CONF_CTRL_API_MODBUS_TCP)"
	cat >$CONF_CTRL_API_MODBUS_TCP <<EOL

Component.target="(&(enabled\=true)(!(service.pid\=Controller.Api.ModbusTcp.ReadOnly.955cac6b-1f87-4f4e-aa8c-15b9520a8f10))(|(id\=_sum)))"
alias="Modbus/TCP\ lesend"
service.factoryPid="Controller.Api.ModbusTcp.ReadOnly"
service.pid="Controller.Api.ModbusTcp.ReadOnly.955cac6b-1f87-4f4e-aa8c-15b9520a8f10"
EOL
}

#
# Setup Controller Api.Websocket
#
function setup_ctrl_api_websocket {
	echo "# Creating $CONF_CTRL_API_WEBSOCKET"
	mkdir -p "$(dirname $CONF_CTRL_API_WEBSOCKET)"
	cat >$CONF_CTRL_API_WEBSOCKET <<EOL
apiTimeout=I"60"
enabled=B"true"
id="ctrlApiWebsocket0"
port=I"8085"
service.factoryPid="Controller.Api.Websocket"
service.pid="Controller.Api.Websocket.9dc51b52-feb6-4f5d-a3b3-5ee6997b20c9"
EOL
}

#
# Setup Controller Limit Total Discharge
#
function setup_ctrl_limit_total_discharge {
	echo "# Creating $CONF_CTRL_LIMIT_TOTAL_DISCHARGE"
	mkdir -p "$(dirname $CONF_CTRL_LIMIT_TOTAL_DISCHARGE)"
	cat >$CONF_CTRL_LIMIT_TOTAL_DISCHARGE <<EOL
enabled=B"true"
ess.id="ess0"
forceChargeSoc=I"10"
id="ctrlLimitTotalDischarge0"
minSoc=I"13"
service.factoryPid="Controller.Ess.LimitTotalDischarge"
service.pid="Controller.Ess.LimitTotalDischarge.9621549c-ece7-4e70-9f48-c685229be4d5"
EOL
}

#
# Setup Controller Limit Total Discharge
#
function setup_ctrl_limit_total_discharge_soltaro_version_c {
	echo "# Creating $CONF_CTRL_LIMIT_TOTAL_DISCHARGE"
	mkdir -p "$(dirname $CONF_CTRL_LIMIT_TOTAL_DISCHARGE)"
	cat >$CONF_CTRL_LIMIT_TOTAL_DISCHARGE <<EOL
enabled=B"true"
ess.id="ess0"
forceChargeSoc=I"5"
id="ctrlLimitTotalDischarge0"
minSoc=I"10"
service.factoryPid="Controller.Ess.LimitTotalDischarge"
service.pid="Controller.Ess.LimitTotalDischarge.9621549c-ece7-4e70-9f48-c685229be4d5"
EOL
}

#
# Setup Controller Balancing Symmetric
#
function setup_ctrl_balancing_symmetric {
	echo "# Creating $CONF_CTRL_BALANCING_SYMMETRIC"
	mkdir -p "$(dirname $CONF_CTRL_BALANCING_SYMMETRIC)"
	cat >$CONF_CTRL_BALANCING_SYMMETRIC <<EOL
enabled=B"true"
ess.id="ess0"
id="ctrlBalancing0"
meter.id="meter0"
service.factoryPid="Controller.Symmetric.Balancing"
service.pid="Controller.Symmetric.Balancing.4e1667dc-ce19-4870-bdb6-475d639d832a"
EOL
}

#
# Setup Timedata Rrd4j
#
function setup_timedata_rrd4j {
	echo "# Creating $CONF_TIMEDATA_RRD4J"
	mkdir -p "$(dirname $CONF_TIMEDATA_RRD4J)"
	cat >$CONF_TIMEDATA_RRD4J <<EOL
alias=""
enabled=B"true"
id="rrd4j0"
recordPeriod=I"60"
service.factoryPid="Timedata.Rrd4j"
service.pid="Timedata.Rrd4j.89268e1c-3704-4329-b9ed-49949807d2fb"
EOL
}

#
# Setup GoodWe ET-Series
#
function setup_pro_hybrid_10_gw_components {
	echo "# Creating $CONF_ESS0_GOODWE_ET"
	mkdir -p "$(dirname $CONF_ESS0_GOODWE_ET)"
	cat >$CONF_ESS0_GOODWE_ET <<EOL
alias="FENECON\\ Pro\\ Hybrid\\ GW"
capacity=I"9000"
enabled=B"true"
id="ess0"
maxBatteryPower=I"5200"
modbus.id="modbus0"
modbusUnitId=I"247"
readOnlyMode=B"true"
service.factoryPid="GoodWe.Ess"
service.pid="GoodWe.Ess.9784f354-1886-4c12-9b17-596d106f95e2"
Modbus.target="(&(enabled\=true)(!(service.pid\=GoodWe.Ess.9784f354-1886-4c12-9b17-596d106f95e2))(|(id\=modbus0)))"
EOL
	echo "# Creating $CONF_CHARGER0_GOODWE_ET"
	mkdir -p "$(dirname $CONF_CHARGER0_GOODWE_ET)"
	cat >$CONF_CHARGER0_GOODWE_ET <<EOL
alias="PV\\ 1"
enabled=B"true"
essOrBatteryInverter.id="ess0"
id="charger0"
modbus.id="modbus0"
modbusUnitId=I"247"
service.factoryPid="GoodWe.Charger-PV1"
service.pid="GoodWe.Charger-PV1.af91ae04-dc4d-45b3-87de-23ab03a4b57e"
Modbus.target="(&(enabled\=true)(!(service.pid\=GoodWe.Charger-PV1.af91ae04-dc4d-45b3-87de-23ab03a4b57e))(|(id\=modbus0)))"
essOrBatteryInverter.target="(&(enabled\=true)(!(service.pid\=GoodWe.Charger-PV1.af91ae04-dc4d-45b3-87de-23ab03a4b57e))(|(id\=ess0)))"
EOL
	echo "# Creating $CONF_CHARGER1_GOODWE_ET"
	mkdir -p "$(dirname $CONF_CHARGER1_GOODWE_ET)"
	cat >$CONF_CHARGER1_GOODWE_ET <<EOL
alias="PV\\ 2"
enabled=B"true"
essOrBatteryInverter.id="ess0"
id="charger1"
modbus.id="modbus0"
modbusUnitId=I"247"
service.factoryPid="GoodWe.Charger-PV2"
service.pid="GoodWe.Charger-PV2.98ec1eae-f286-445e-8dbe-7ebe9e6c1ac6"
Modbus.target="(&(enabled\=true)(!(service.pid\=GoodWe.Charger-PV2.98ec1eae-f286-445e-8dbe-7ebe9e6c1ac6))(|(id\=modbus0)))"
essOrBatteryInverter.target="(&(enabled\=true)(!(service.pid\=GoodWe.Charger-PV2.98ec1eae-f286-445e-8dbe-7ebe9e6c1ac6))(|(id\=ess0)))"
EOL
	echo "# Creating $CONF_METER0_GOODWE_ET"
	mkdir -p "$(dirname $CONF_METER0_GOODWE_ET)"
	cat >$CONF_METER0_GOODWE_ET <<EOL
alias="Netzzähler"
enabled=B"true"
id="meter0"
modbus.id="modbus0"
modbusUnitId=I"247"
service.factoryPid="GoodWe.Grid-Meter"
service.pid="GoodWe.Grid-Meter.af707ccf-0e4c-41f2-9cac-6dfd8295df33"
Modbus.target="(&(enabled\=true)(!(service.pid\=GoodWe.Grid-Meter.af707ccf-0e4c-41f2-9cac-6dfd8295df33))(|(id\=modbus0)))"
EOL
}

#
# Setup FENECON HOME GoodWe
#
function setup_fenecon_home_gw_components {
	echo "# Creating $CONF_BATTERY_INVERTER0_GOODWE"
	mkdir -p "$(dirname $CONF_BATTERY_INVERTER0_GOODWE)"
	cat >$CONF_BATTERY_INVERTER0_GOODWE <<EOL
alias="GoodWe\\ Batterie-WR"
id="batteryInverter0"
enabled=B"true"
modbus.id="modbus1"
modbusUnitId=I"247"
Modbus.target="(&(enabled\=true)(!(service.pid\=GoodWe.BatteryInverter.9e6e0862-2d28-4747-89d2-963c1cef9861))(|(id\=modbus1)))"
service.factoryPid="GoodWe.BatteryInverter"
service.pid="GoodWe.BatteryInverter.9e6e0862-2d28-4747-89d2-963c1cef9861"
EOL
	echo "# Creating $CONF_CHARGER0_GOODWE_ET"
	mkdir -p "$(dirname $CONF_CHARGER0_GOODWE_ET)"
	cat >$CONF_CHARGER0_GOODWE_ET <<EOL
alias="PV\ Produktion\ 1"
enabled=B"true"
essOrBatteryInverter.id="batteryInverter0"
id="charger0"
modbus.id="modbus1"
modbusUnitId=I"247"
service.factoryPid="GoodWe.Charger-PV1"
service.pid="GoodWe.Charger-PV1.af91ae04-dc4d-45b3-87de-23ab03a4b57e"
Modbus.target="(&(enabled\=true)(!(service.pid\=GoodWe.Charger-PV1.af91ae04-dc4d-45b3-87de-23ab03a4b57e))(|(id\=modbus1)))"
essOrBatteryInverter.target="(&(enabled\=true)(!(service.pid\=GoodWe.Charger-PV1.af91ae04-dc4d-45b3-87de-23ab03a4b57e))(|(id\=batteryInverter0)))"
EOL
	echo "# Creating $CONF_CHARGER1_GOODWE_ET"
	mkdir -p "$(dirname $CONF_CHARGER1_GOODWE_ET)"
	cat >$CONF_CHARGER1_GOODWE_ET <<EOL
	
alias="PV\ Produktion\ 2"
enabled=B"true"
essOrBatteryInverter.id="batteryInverter0"
id="charger1"
modbus.id="modbus1"
modbusUnitId=I"247"
service.factoryPid="GoodWe.Charger-PV2"
service.pid="GoodWe.Charger-PV2.98ec1eae-f286-445e-8dbe-7ebe9e6c1ac6"
Modbus.target="(&(enabled\=true)(!(service.pid\=GoodWe.Charger-PV2.98ec1eae-f286-445e-8dbe-7ebe9e6c1ac6))(|(id\=modbus1)))"
essOrBatteryInverter.target="(&(enabled\=true)(!(service.pid\=GoodWe.Charger-PV2.98ec1eae-f286-445e-8dbe-7ebe9e6c1ac6))(|(id\=batteryInverter0)))"
EOL
	echo "# Creating $CONF_METER0_GOODWE_ET"
	mkdir -p "$(dirname $CONF_METER0_GOODWE_ET)"
	cat >$CONF_METER0_GOODWE_ET <<EOL
	
alias="Netzzähler"
enabled=B"true"
id="meter0"
modbus.id="modbus1"
modbusUnitId=I"247"
service.factoryPid="GoodWe.Grid-Meter"
service.pid="GoodWe.Grid-Meter.af707ccf-0e4c-41f2-9cac-6dfd8295df33"
Modbus.target="(&(enabled\=true)(!(service.pid\=GoodWe.Grid-Meter.af707ccf-0e4c-41f2-9cac-6dfd8295df33))(|(id\=modbus1)))"
EOL
}

function setup_ctrl_surplus_feed_to_grid_limit {
	echo "# Creating $CONF_CTRL_ESS_SURPULUS_FEED_TO_GRID0"
	mkdir -p "$(dirname $CONF_CTRL_ESS_SURPULUS_FEED_TO_GRID0)"
	cat >$CONF_CTRL_ESS_SURPULUS_FEED_TO_GRID0 <<EOL
alias="Überschusseinspeisung"
enabled=B"true"
ess.id="ess0"
id="ctrlEssSurplusFeedToGrid0"
service.factoryPid="Controller.Ess.Hybrid.Surplus-Feed-To-Grid"
service.pid="Controller.Ess.Hybrid.Surplus-Feed-To-Grid.cd963733-b33e-47dd-a77d-382532783519"
ess.target="(&(enabled\=true)(!(service.pid\=Controller.Ess.Hybrid.Surplus-Feed-To-Grid.cd963733-b33e-47dd-a77d-382532783519))(|(id\=ess0)))"
EOL
}

function setup_relay_kmtronik_4port {
	echo "# Creating $CONF_RELAY_KMTRONIK_4PORT_IO0"
	mkdir -p "$(dirname $CONF_RELAY_KMTRONIK_4PORT_IO0)"
	cat >$CONF_RELAY_KMTRONIK_4PORT_IO0 <<EOL
alias="Relaisboard"
id="io0"
enabled=B"true"
modbus.id="modbus0"
modbusUnitId=I"1"
service.factoryPid="IO.KMtronic.4Port"
service.pid="IO.KMtronic.4Port.bd49fc20-b806-4b38-a0c9-c559470c4f54"
Modbus.target="(&(enabled\=true)(!(service.pid\=IO.KMtronic.4Port.bd49fc20-b806-4b38-a0c9-c559470c4f54))(|(id\=modbus0)))"
EOL
}

function setup_relay_kmtronik_8port {
	echo "# Creating $CONF_RELAY_KMTRONIK_IO0"
	mkdir -p "$(dirname $CONF_RELAY_KMTRONIK_IO0)"
	cat >$CONF_RELAY_KMTRONIK_IO0 <<EOL
alias="Relaisboard"
id="io0"
Modbus.target="(&(enabled\=true)(!(service.pid\=IO.KMtronic.3e636f4f-5a1e-4bdd-8b2d-160e08c55f34))(|(id\=modbus0)))"
modbusUnitId=I"6"
service.factoryPid="IO.KMtronic"
service.pid="IO.KMtronic.3e636f4f-5a1e-4bdd-8b2d-160e08c55f34"
EOL
}

###########
# OFFGRID
###########

function setup_modbus3_wago_offgrid_tcp_192_168_1_50 {
	echo "# Creating $CONF_RELAY_WAGO_MODBUS3_TCP_192_168_1_50"
	mkdir -p "$(dirname $CONF_RELAY_WAGO_MODBUS3_TCP_192_168_1_50)"
	cat >$CONF_RELAY_WAGO_MODBUS3_TCP_192_168_1_50 <<EOL
alias="Kommunikation\\ mit\\ dem\\ WAGO\\ Relaisboard"
enabled=B"true"
id="modbus3"
invalidateElementsAfterReadErrors=I"3"
ip="192.168.1.50"
logVerbosity="NONE"
port=I"502"
service.factoryPid="Bridge.Modbus.Tcp"
service.pid="Bridge.Modbus.Tcp.4238ca19-8151-47fb-a9f6-78d213a697b2"
EOL
}

function setup_offgrid_io_wago {
echo "# Creating $CONF_RELAY_WAGO_IO0"
	mkdir -p "$(dirname $CONF_RELAY_WAGO_IO0)"
	cat >$CONF_RELAY_WAGO_IO0 <<EOL
Modbus.target="(&(enabled\=true)(!(service.pid\=IO.WAGO.1c6498cd-1bbd-427e-a9a0-6fabc67a88d2))(|(id\=modbus3)))"
id="io0"
modbus.id="modbus3"
service.factoryPid="IO.WAGO"
service.pid="IO.WAGO.1c6498cd-1bbd-427e-a9a0-6fabc67a88d2"
EOL
}

function setup_offgrid_offGridSwitch {
echo "# Creating $CONF_OFFGRID_SWITCH"
	mkdir -p "$(dirname $CONF_OFFGRID_SWITCH)"
	cat >$CONF_OFFGRID_SWITCH <<EOL
service.factoryPid="Io.Off.Grid.Switch"
service.pid="Io.Off.Grid.Switch.65163c6a-0833-4c3f-8061-cdea678a0714"
EOL
}

#
# ESS generic offgrid
#
function setup_ess0_generic_offgrid {
	echo "# Creating $CONF_ESS_GENERIC_OFFGRID"
	mkdir -p "$(dirname $CONF_ESS_GENERIC_OFFGRID)"
	cat >$CONF_ESS_GENERIC_OFFGRID <<EOL
battery.id="bms0"
battery.target="(&(enabled\=true)(!(service.pid\=Ess.Generic.OffGrid.37d1bdb9-a33d-422e-a05b-b196af282f7f))(|(id\=bms0)))"
batteryInverter.target="(&(enabled\=true)(!(service.pid\=Ess.Generic.OffGrid.37d1bdb9-a33d-422e-a05b-b196af282f7f))(|(id\=batteryInverter0)))"
offGridSwitch.target="(&(enabled\=true)(!(service.pid\=Ess.Generic.OffGrid.37d1bdb9-a33d-422e-a05b-b196af282f7f))(|(id\=offGridSwitch0)))"
service.factoryPid="Ess.Generic.OffGrid"
service.pid="Ess.Generic.OffGrid.37d1bdb9-a33d-422e-a05b-b196af282f7f"
EOL
}

function setup_ctrl_emergency_capacity_reserve {
	echo "# Creating $CONF_CTRL_EMERGENCY_CAPACITY_RESERVE"
	mkdir -p "$(dirname $CONF_CTRL_EMERGENCY_CAPACITY_RESERVE)"
	cat >$CONF_CTRL_EMERGENCY_CAPACITY_RESERVE <<EOL
ess.target="(&(enabled\=true)(!(service.pid\=Controller.Ess.EmergencyCapacityReserve.e0ba42f5-13e5-4684-af42-6e91c4361d15))(|(id\=ess0)))"
service.factoryPid="Controller.Ess.EmergencyCapacityReserve"
service.pid="Controller.Ess.EmergencyCapacityReserve.e0ba42f5-13e5-4684-af42-6e91c4361d15"
EOL
}

function setup_dess_components {
	echo "# Creating $CONF_CHARGER0_DESS"
	mkdir -p "$(dirname $CONF_CHARGER0_DESS)"
	cat >$CONF_CHARGER0_DESS <<EOL
Modbus.target="(&(enabled\=true)(!(service.pid\=Fenecon.Dess.Charger1.0d3c54c2-50fe-43e9-bfcf-f7e29420f515))(|(id\=modbus0)))"
enabled=B"true"
id="charger0"
modbus.id="modbus0"
service.factoryPid="Fenecon.Dess.Charger1"
service.pid="Fenecon.Dess.Charger1.0d3c54c2-50fe-43e9-bfcf-f7e29420f515"
EOL
	echo "# Creating $CONF_CHARGER1_DESS"
	mkdir -p "$(dirname $CONF_CHARGER1_DESS)"
	cat >$CONF_CHARGER1_DESS <<EOL
Modbus.target="(&(enabled\=true)(!(service.pid\=Fenecon.Dess.Charger2.d5bd8868-944b-45da-9a73-7f8e8931ed50))(|(id\=modbus0)))"
enabled=B"true"
id="charger1"
modbus.id="modbus0"
service.factoryPid="Fenecon.Dess.Charger2"
service.pid="Fenecon.Dess.Charger2.d5bd8868-944b-45da-9a73-7f8e8931ed50"
EOL
	echo "# Creating $CONF_ESS0_DESS"
	mkdir -p "$(dirname $CONF_ESS0_DESS)"
	cat >$CONF_ESS0_DESS <<EOL
Modbus.target="(&(enabled\=true)(!(service.pid\=Fenecon.Dess.Ess.bd252265-3f6e-4609-b362-61f26e87b6c4))(|(id\=modbus0)))"
enabled=B"true"
id="ess0"
modbus.id="modbus0"
service.factoryPid="Fenecon.Dess.Ess"
service.pid="Fenecon.Dess.Ess.bd252265-3f6e-4609-b362-61f26e87b6c4"
EOL
	echo "# Creating $CONF_METER0_DESS"
	mkdir -p "$(dirname $CONF_METER0_DESS)"
	cat >$CONF_METER0_DESS <<EOL
Modbus.target="(&(enabled\=true)(!(service.pid\=Fenecon.Dess.GridMeter.369dee25-e723-4699-8b47-40a4e5a80791))(|(id\=modbus0)))"
enabled=B"true"
id="meter0"
modbus.id="modbus0"
service.factoryPid="Fenecon.Dess.GridMeter"
service.pid="Fenecon.Dess.GridMeter.369dee25-e723-4699-8b47-40a4e5a80791"
EOL
	echo "# Creating $CONF_METER1_DESS"
	mkdir -p "$(dirname $CONF_METER1_DESS)"
	cat >$CONF_METER1_DESS <<EOL
Modbus.target="(&(enabled\=true)(!(service.pid\=Fenecon.Dess.PvMeter.56b6c914-6ce8-4219-a9d8-84f54b4ebea4))(|(id\=modbus0)))"
enabled=B"true"
id="meter1"
modbus.id="modbus0"
service.factoryPid="Fenecon.Dess.PvMeter"
service.pid="Fenecon.Dess.PvMeter.56b6c914-6ce8-4219-a9d8-84f54b4ebea4"
EOL
}

#
# Setup FENECON Pro Hybrid 10-Serie Components
#
function setup_pro_hybrid_10_components {
	echo "# Creating $CONF_CORE_PROHYBRID10"
	mkdir -p "$(dirname $CONF_CORE_PROHYBRID10)"
	cat >$CONF_CORE_PROHYBRID10 <<EOL
enabled=B"true"
id="kacoCore0"
service.factoryPid="Kaco.BlueplanetHybrid10.Core"
service.pid="Kaco.BlueplanetHybrid10.Core.b11f6d44-631d-49a7-a9eb-fd063aa1ed3e"
userkey="user"
EOL
	echo "# Creating $CONF_ESS0_PROHYBRID10"
	mkdir -p "$(dirname $CONF_ESS0_PROHYBRID10)"
	cat >$CONF_ESS0_PROHYBRID10 <<EOL
core.id="kacoCore0"
core.target="(&(enabled\=true)(!(service.pid\=Kaco.BlueplanetHybrid10.Ess.0bb53c70-83ff-40ba-b351-1c4df781ab4f))(|(id\=kacoCore0)))"
enabled=B"true"
id="ess0"
maxP=I"4000"
readOnly=B"true"
service.factoryPid="Kaco.BlueplanetHybrid10.Ess"
service.pid="Kaco.BlueplanetHybrid10.Ess.0bb53c70-83ff-40ba-b351-1c4df781ab4f"
EOL
	echo "# Creating $CONF_METER0_PROHYBRID10"
	mkdir -p "$(dirname $CONF_METER0_PROHYBRID10)"
	cat >$CONF_METER0_PROHYBRID10 <<EOL
core.id="kacoCore0"
core.target="(&(enabled\=true)(!(service.pid\=Kaco.BlueplanetHybrid10.GridMeter.008bcc03-a824-4cad-9fcd-993bbbb6f4ad))(|(id\=kacoCore0)))"
enabled=B"true"
id="meter0"
service.factoryPid="Kaco.BlueplanetHybrid10.GridMeter"
service.pid="Kaco.BlueplanetHybrid10.GridMeter.008bcc03-a824-4cad-9fcd-993bbbb6f4ad"
EOL
	echo "# Creating $CONF_CHARGER0_PROHYBRID10"
	mkdir -p "$(dirname $CONF_CHARGER0_PROHYBRID10)"
	cat >$CONF_CHARGER0_PROHYBRID10 <<EOL
core.id="kacoCore0"
core.target="(&(enabled\=true)(!(service.pid\=Kaco.BlueplanetHybrid10.Charger.30b51193-e256-42b9-a743-b1c043c713fa))(|(id\=kacoCore0)))"
enabled=B"true"
id="charger0"
service.factoryPid="Kaco.BlueplanetHybrid10.Charger"
service.pid="Kaco.BlueplanetHybrid10.Charger.30b51193-e256-42b9-a743-b1c043c713fa"
EOL
}

function setup_modbus1_tcp_10_4_0_15 {
	echo "# Creating $CONF_MODBUS1_TCP_10_4_0_15"
	mkdir -p "$(dirname $CONF_MODBUS1_TCP_10_4_0_15)"
	cat >$CONF_MODBUS1_TCP_10_4_0_15 <<EOL
enabled=B"true"
id="modbus1"
ip="10.4.0.15"
service.factoryPid="Bridge.Modbus.Tcp"
service.pid="Bridge.Modbus.Tcp.129c3073-8bb6-4f55-911c-966092696cd5"
EOL

# Hinzufügen der IP Adresse
if [ "$(grep -c 'Address=10.4.0.1/16' /etc/systemd/network/eth1.network)" -eq "0" ]; then 
	echo "# Konfiguriere IP-Adresse 10.4.0.1/16"
	echo "Address=10.4.0.1/16" >> /etc/systemd/network/eth1.network
	systemctl restart systemd-networkd
	systemctl enable systemd-networkd
else
	echo "# IP-Adresse 10.4.0.1/16 ist bereits konfiguriert"
fi
}

function setup_modbus1_tcp_10_4_0_10 {
	echo "# Creating $CONF_MODBUS1_TCP_10_4_0_10"
	mkdir -p "$(dirname $CONF_MODBUS1_TCP_10_4_0_10)"
	cat >$CONF_MODBUS1_TCP_10_4_0_10 <<EOL
alias="Kommunikation\\ mit\\ dem\\ Wechselrichter"
enabled=B"true"
id="modbus1"
invalidateElementsAfterReadErrors=I"3"
ip="10.4.0.10"
logVerbosity="NONE"
port=I"502"
service.factoryPid="Bridge.Modbus.Tcp"
service.pid="Bridge.Modbus.Tcp.faf61c1f-9327-4edc-bf35-8280bb8fa94f"
EOL

# Hinzufügen der IP Adresse
if [ "$(grep -c 'Address=10.4.0.1/16' /etc/systemd/network/eth1.network)" -eq "0" ]; then 
	echo "# Konfiguriere IP-Adresse 10.4.0.1/16"
	echo "Address=10.4.0.1/16" >> /etc/systemd/network/eth1.network
	systemctl restart systemd-networkd
	systemctl enable systemd-networkd
else
	echo "# IP-Adresse 10.4.0.1/16 ist bereits konfiguriert"
fi
}

function setup_modbus1_tcp_192_168_1_11 {
	echo "# Creating $CONF_MODBUS1_TCP_192_168_1_11"
	mkdir -p "$(dirname $CONF_MODBUS1_TCP_192_168_1_11)"
	cat >$CONF_MODBUS1_TCP_192_168_1_11 <<EOL
alias="Kommunikation\\ mit\\ dem\\ Wechselrichter"
enabled=B"true"
id="modbus1"
invalidateElementsAfterReadErrors=I"3"
ip="192.168.1.11"
logVerbosity="NONE"
port=I"502"
service.factoryPid="Bridge.Modbus.Tcp"
service.pid="Bridge.Modbus.Tcp.39f1f42b-45af-4afd-a412-a27e5434e8f9"
EOL


echo "# Überprüfe enx Netzwerkdatei"
if [ "$(ls /etc/systemd/network | grep -c enx.network)" -eq "1" ]; then

	if [ "$(grep -c 'Address=192.168.1.9/29' /etc/systemd/network/enx.network)" -eq "1" ]; then 
	
		echo "# Ändere die IP-Adresse von 192.168.1.9/29 zu 192.168.1.9/26"
		sed -i -e "s/$192.168.1.9\/29\b/$192.168.1.9\/26/g" /etc/systemd/network/enx.network
		systemctl restart systemd-networkd
		systemctl enable systemd-networkd
	else
		if [ "$(grep -c 'Address=192.168.1.10/29' /etc/systemd/network/enx.network)" -eq "1" ]; then 
			echo "# Ändere die IP-Adresse von 192.168.1.10/29 zu 192.168.1.9/26"
			sed -i -e "s/$192.168.1.10\/29\b/$192.168.1.9\/26/g" /etc/systemd/network/enx.network
			systemctl restart systemd-networkd
			systemctl enable systemd-networkd
		else
			if [ "$(grep -c 'Address=192.168.1.9/26' /etc/systemd/network/enx.network)" -eq "0" ]; then 
				echo "#Hinzufügen der IP-Adresse 192.168.1.9/26"
				echo "Address=192.168.1.9/26" >> /etc/systemd/network/enx.network
				systemctl restart systemd-networkd
				systemctl enable systemd-networkd
			else
				echo "#IP-Adresse 192.168.1.9/26 existiert bereits"
			fi
		fi
	fi
else 
	echo "# Überprüfe eth1 Netzwerkdatei"
	if [ "$(ls /etc/systemd/network | grep -c eth1.network)" -eq "1" ]; then

		if [ "$(grep -c 'Address=192.168.1.9/29' /etc/systemd/network/eth1.network)" -eq "1" ]; then 
	
			echo "# Ändere die IP-Adresse von 192.168.1.9/29 zu 192.168.1.9/26"
			sed -i -e "s/$192.168.1.9\/29\b/$192.168.1.9\/26/g" /etc/systemd/network/eth1.network
			systemctl restart systemd-networkd
			systemctl enable systemd-networkd
		else
			if [ "$(grep -c 'Address=192.168.1.10/29' /etc/systemd/network/eth1.network)" -eq "1" ]; then 
				echo "# Ändere die IP-Adresse von 192.168.1.10/29 zu 192.168.1.9/26"
				sed -i -e "s/$192.168.1.10\/29\b/$192.168.1.9\/26/g" /etc/systemd/network/eth1.network
				systemctl restart systemd-networkd
				systemctl enable systemd-networkd
			else
				if [ "$(grep -c 'Address=192.168.1.9/26' /etc/systemd/network/eth1.network)" -eq "0" ]; then 
					echo "#Hinzufügen der IP-Adresse 192.168.1.9/26"
					echo "Address=192.168.1.9/26" >> /etc/systemd/network/eth1.network
					systemctl restart systemd-networkd
					systemctl enable systemd-networkd
				else
					echo "#IP-Adresse 192.168.1.9/26 existiert bereits"
				fi
			fi
		fi
	fi
fi

# Hinzufügen der IP Adresse
#if [ "$(grep -c 'Address=192.168.1.9/29' /etc/systemd/network/eth1.network)" -eq "0" ]; then 
#	if [ "$(grep -c 'Address=192.168.1.10/29' /etc/systemd/network/eth1.network)" -eq "0" ]; then 
#		echo "# Konfiguriere IP-Adresse 192.168.1.9/29"
#		echo "Address=192.168.1.9/29" >> /etc/systemd/network/eth1.network
#		systemctl restart systemd-networkd
#		systemctl enable systemd-networkd
#	else
#		echo "# IP-Adresse 192.168.1.10/29 ist bereits konfiguriert"
#	fi
#else
#	echo "# IP-Adresse 192.168.1.9/29 ist bereits konfiguriert"
#fi
}

function setup_ess0_commercial_40 {
	echo "# Creating $CONF_ESS0_COMMERCIAL_40"
	mkdir -p "$(dirname $CONF_ESS0_COMMERCIAL_40)"
	cat >$CONF_ESS0_COMMERCIAL_40 <<EOL
Modbus.target="(&(enabled\=true)(!(service.pid\=Ess.Fenecon.Commercial40.2032b36c-f232-4463-9458-2fd1a9f19f33))(|(id\=modbus1)))"
enabled=B"true"
id="ess0"
modbus.id="modbus1"
readOnlyMode=B"false"
service.factoryPid="Ess.Fenecon.Commercial40"
service.pid="Ess.Fenecon.Commercial40.2032b36c-f232-4463-9458-2fd1a9f19f33"
EOL
}

function setup_charger0_commercial_40 {
	echo "# Creating $CONF_CHARGER0_COMMERCIAL_40"
	mkdir -p "$(dirname $CONF_CHARGER0_COMMERCIAL_40)"
	cat >$CONF_CHARGER0_COMMERCIAL_40 <<EOL
Ess.target="(&(enabled\=true)(!(service.pid\=EssDcCharger.Fenecon.Commercial40.e5912787-a377-49f5-af43-2bfe0ccf9ad7))(|(id\=ess0)))"
Modbus.target="(&(enabled\=true)(!(service.pid\=EssDcCharger.Fenecon.Commercial40.e5912787-a377-49f5-af43-2bfe0ccf9ad7))(|(id\=modbus1)))"
alias=""
enabled=B"true"
ess.id="ess0"
id="charger0"
service.factoryPid="EssDcCharger.Fenecon.Commercial40"
service.pid="EssDcCharger.Fenecon.Commercial40.e5912787-a377-49f5-af43-2bfe0ccf9ad7"
EOL
}

function setup_ctrl_commercial_40_surplus {
	echo "# Creating $CONF_CTRL_COMMERCIAL_40_SURPLUS"
	mkdir -p "$(dirname $CONF_CTRL_COMMERCIAL_40_SURPLUS)"
	cat >$CONF_CTRL_COMMERCIAL_40_SURPLUS <<EOL
alias=""
allowedChargePowerLimit=I"-10000"
charger.id="charger0"
enabled=B"true"
ess.id="ess0"
id="ctrlCommercial40SurplusFeedIn0"
increasePowerFactor=D"4607632778762754458"
maxIncreasePowerFactor=I"2000"
offTime="17:00:00"
pvLimitOnPowerDecreaseCausedByOvertemperature=I"35000"
service.factoryPid="Controller.Ess.FeneconCommercial40SurplusFeedIn"
service.pid="Controller.Ess.FeneconCommercial40SurplusFeedIn.20b5c675-baa6-46e7-9be2-f13b5609d25c"
socLimit=I"85"
EOL
}

#
# KOSTAL PIKO
#
function setup_kostal_piko_components {
	echo "# Creating $CONF_KOSTAL_PIKO_CORE"
	mkdir -p "$(dirname $CONF_KOSTAL_PIKO_CORE)"
	cat >$CONF_KOSTAL_PIKO_CORE <<EOL
alias=""
enabled=B"true"
id="kostalPiko0"
ip="192.168.0.1"
port=I"81"
service.factoryPid="Kostal.Piko.Core"
service.pid="Kostal.Piko.Core.14f7de38-e86e-489c-acfb-2ed15476c530"
unitID=I"255"
EOL
	echo "# Creating $CONF_KOSTAL_PIKO_GRIDMETER"
	mkdir -p "$(dirname $CONF_KOSTAL_PIKO_GRIDMETER)"
	cat >$CONF_KOSTAL_PIKO_GRIDMETER <<EOL
alias=""
core.id="kostalPiko0"
enabled=B"true"
id="meter0"
service.factoryPid="Kostal.Piko.GridMeter"
service.pid="Kostal.Piko.GridMeter.24d87e3f-904b-4473-83cf-e08046833f57"
EOL
	echo "# Creating $CONF_KOSTAL_PIKO_ESS"
	mkdir -p "$(dirname $CONF_KOSTAL_PIKO_ESS)"
	cat >$CONF_KOSTAL_PIKO_ESS <<EOL
alias=""
core.id="kostalPiko0"
enabled="true"
id="ess0"
service.factoryPid="Kostal.Piko.Ess"
service.pid="Kostal.Piko.Ess.28f7ea70-95ec-42d1-987a-7b4784bdc7d0"
EOL
	echo "# Creating $CONF_KOSTAL_PIKO_CHARGER"
	mkdir -p "$(dirname $CONF_KOSTAL_PIKO_CHARGER)"
	cat >$CONF_KOSTAL_PIKO_CHARGER <<EOL
alias=""
core.id="kostalPiko0"
enabled="true"
id="charger0"
service.factoryPid="Kostal.Piko.Charger"
service.pid="Kostal.Piko.Charger.291f3b7b-c12c-40c3-8927-610b9c725c44"
EOL
}

#
# Setup scheduler configuration for Commercial 40 DC with Surplus-Feed-In
#
function setup_scheduler_commercial_40_surplus {
	echo "# Creating $CONF_SCHEDULER"
	mkdir -p "$(dirname $CONF_SCHEDULER)"
	cat >$CONF_SCHEDULER <<EOL
controllers.ids=[ \
  "ctrlLimitTotalDischarge0", \
  "ctrlCommercial40SurplusFeedIn0", \
  "ctrlBalancing0", \
  ]
cycleTime=I"1000"
enabled=B"true"
id="scheduler0"
service.factoryPid="Scheduler.AllAlphabetically"
service.pid="Scheduler.AllAlphabetically.d0ec9992-e625-4ee4-b252-c9d9d417d78c"
EOL
}

#
# Soltaro Batterie single rack - Version B
#
function setup_bms0_soltaro_single_rack {
	echo "# Creating $CONF_BMS0_SOLTARO_SINGLE_RACK"
	mkdir -p "$(dirname $CONF_BMS0_SOLTARO_SINGLE_RACK)"
	cat >$CONF_BMS0_SOLTARO_SINGLE_RACK <<EOL
Modbus.target="(&(enabled\=true)(!(service.pid\=Bms.Soltaro.SingleRack.VersionB.09f38574-b9d7-48ba-a993-110b7f5810a3))(|(id\=modbus0)))"
ReduceTasks=B"false"
SoCLowAlarm=I"0"
alias=""
batteryState="DEFAULT"
enabled=B"true"
errorLevel2Delay=I"600"
id="bms0"
maxStartAppempts=I"5"
maxStartTime=I"30"
minimalCellVoltage=I"2800"
modbus.id="modbus0"
modbusUnitId=I"0"
numberOfSlaves=I"20"
pendingTolerance=I"15"
service.factoryPid="Bms.Soltaro.SingleRack.VersionB"
service.pid="Bms.Soltaro.SingleRack.VersionB.09f38574-b9d7-48ba-a993-110b7f5810a3"
startUnsuccessfulDelay=I"3600"
watchdog=I"60"
EOL
}

#
# Soltaro Batterie single rack - Version C
#
function setup_bms0_soltaro_single_rack_version_c {
	echo "# Creating $CONF_BMS0_SOLTARO_SINGLE_RACK_VERSION_C"
	mkdir -p "$(dirname $CONF_BMS0_SOLTARO_SINGLE_RACK_VERSION_C)"
	cat >$CONF_BMS0_SOLTARO_SINGLE_RACK_VERSION_C <<EOL
Modbus.target="(&(enabled\=true)(!(service.pid\=Bms.Soltaro.SingleRack.VersionC.5b046fa5-a089-4239-a08a-7cc7927fb56e))(|(id\=modbus0)))"
SocLowAlarm="0"
alias="Batterie"
enabled=B"true"
errorLevel2Delay=I"600"
id="bms0"
minimalCellVoltage=I"2800"
modbus.id="modbus0"
modbusUnitId="1"
moduleType="MODULE_3_5_KWH"
numberOfSlaves=I"20"
pendingTolerance=I"15"
service.factoryPid="Bms.Soltaro.SingleRack.VersionC"
service.pid="Bms.Soltaro.SingleRack.VersionC.5b046fa5-a089-4239-a08a-7cc7927fb56e"
startStop="START"
startUnsuccessfulDelay=I"3600"
watchdog=I"90"
EOL
}

#
# Soltaro Batterie single rack - Version C
# 9 Modules for commercial 30
#
function setup_bms0_soltaro_single_rack_version_c_comm30 {
	echo "# Creating $CONF_BMS0_SOLTARO_SINGLE_RACK_VERSION_C"
	mkdir -p "$(dirname $CONF_BMS0_SOLTARO_SINGLE_RACK_VERSION_C)"
	cat >$CONF_BMS0_SOLTARO_SINGLE_RACK_VERSION_C <<EOL
Modbus.target="(&(enabled\=true)(!(service.pid\=Bms.Soltaro.SingleRack.VersionC.5b046fa5-a089-4239-a08a-7cc7927fb56e))(|(id\=modbus0)))"
SocLowAlarm=I"0"
alias="Batterie"
enabled=B"true"
errorLevel2Delay=I"600"
id="bms0"
minimalCellVoltage=I"2800"
modbus.id="modbus0"
modbusUnitId="1"
moduleType="MODULE_3_5_KWH"
numberOfSlaves=I"9"
pendingTolerance=I"15"
service.factoryPid="Bms.Soltaro.SingleRack.VersionC"
service.pid="Bms.Soltaro.SingleRack.VersionC.5b046fa5-a089-4239-a08a-7cc7927fb56e"
startStop="AUTO"
startUnsuccessfulDelay=I"3600"
watchdog=I"90"
EOL
}

#
# Batterie soltaro GEN_2
# Battery/Fenecon/Commercial/8b16b276-963b-4cc0-96a1-bc7b68058222.config
#
function setup_battery0_soltaro_gen2 {
	echo "# Creating $CONF_BATTERY_SOLTARO_GEN_2"
	mkdir -p "$(dirname $CONF_BATTERY_SOLTARO_GEN_2)"
	cat >$CONF_BATTERY_SOLTARO_GEN_2 <<EOL
Modbus.target="(&(enabled\=true)(!(service.pid\=Battery.Fenecon.Commercial.8b16b276-963b-4cc0-96a1-bc7b68058222))(|(id\=modbus0)))"
batteryStartStopRelay="io0/Relay8"
enabled=B"true"
modbusUnitId=I"3"
service.factoryPid="Battery.Fenecon.Commercial"
service.pid="Battery.Fenecon.Commercial.8b16b276-963b-4cc0-96a1-bc7b68058222"
alias="Batterie"
id="battery0"
EOL
}

#
# Soltaro Batterie single rack - Version C new
# 9 Modules for commercial 30
#
function setup_bms0_soltaro_single_rack_version_c_comm30_2021_17_1 {
	echo "# Creating $CONF_BMS0_SOLTARO_SINGLE_RACK_VERSION_C"
	mkdir -p "$(dirname $CONF_BMS0_SOLTARO_SINGLE_RACK_VERSION_C)"
	cat >$CONF_BMS0_SOLTARO_SINGLE_RACK_VERSION_C <<EOL
Modbus.target="(&(enabled\=true)(!(service.pid\=Bms.Soltaro.SingleRack.VersionC.5b046fa5-a089-4239-a08a-7cc7927fb56e))(|(id\=modbus0)))"
alias="Batterie"
enabled=B"true"
id="bms0"
modbus.id="modbus0"
modbusUnitId="1"
service.factoryPid="Bms.Soltaro.SingleRack.VersionC"
service.pid="Bms.Soltaro.SingleRack.VersionC.5b046fa5-a089-4239-a08a-7cc7927fb56e"
startStop="AUTO"
EOL
}

#
# ATL Batterie for FENECON HOME
#
function setup_battery0_fenecon_home {
	echo "# Creating $CONF_BATTERY_FENECON_HOME"
	mkdir -p "$(dirname $CONF_BATTERY_FENECON_HOME)"
	cat >$CONF_BATTERY_FENECON_HOME <<EOL	
alias="Batterie"
enabled=B"true"
id="battery0"
modbus.id="modbus0"
modbusUnitId=I"1"
startStop="AUTO"
service.factoryPid="Battery.Fenecon.Home"
service.pid="Battery.Fenecon.Home.6b2b91c4-b68d-4c76-b6b4-f811a7999e29"
Modbus.target="(&(enabled\=true)(!(service.pid\=Battery.Fenecon.Home.6b2b91c4-b68d-4c76-b6b4-f811a7999e29))(|(id\=modbus0)))"
EOL
}

#
# Kaco 50 Battery-Inverter old ess version (bms + inverter in one)
#
function setup_ess0_kaco_blueplanet_gridsave_50 {
	echo "# Creating $CONF_ESS0_KACO_BLUEPLANET_GRIDSAVE_50"
	mkdir -p "$(dirname $CONF_ESS0_KACO_BLUEPLANET_GRIDSAVE_50)"
	cat >$CONF_ESS0_KACO_BLUEPLANET_GRIDSAVE_50 <<EOL
Battery.target="(&(enabled\=true)(!(service.pid\=Ess.Kaco.BlueplanetGridsave50.22a16c3b-0147-41a4-8753-98de6860b339))(|(id\=bms0)))"
Modbus.target="(&(enabled\=true)(!(service.pid\=Ess.Kaco.BlueplanetGridsave50.22a16c3b-0147-41a4-8753-98de6860b339))(|(id\=modbus1)))"
alias=""
battery.id="bms0"
enabled=B"true"
id="ess0"
modbus.id="modbus1"
service.factoryPid="Ess.Kaco.BlueplanetGridsave50"
service.pid="Ess.Kaco.BlueplanetGridsave50.22a16c3b-0147-41a4-8753-98de6860b339"
watchdoginterval=I"0"
EOL
}

#
# KACO blueplanet battery inverter.
#
function setup_battery_inverter_kaco_blueplanet_gridsave {
	echo "# Creating $CONF_BATTERY_INVERTER0_KACO_BLUEPLANET_GRIDSAVE"
	mkdir -p "$(dirname $CONF_BATTERY_INVERTER0_KACO_BLUEPLANET_GRIDSAVE)"
	cat >$CONF_BATTERY_INVERTER0_KACO_BLUEPLANET_GRIDSAVE <<EOL
Modbus.target="(&(enabled\=true)(!(service.pid\=Battery-Inverter.Kaco.BlueplanetGridsave.a42c2cec-2850-47f7-a1ae-fc4decdbb2c0))(|(id\=modbus1)))"
activateWatchdog=B"true"
alias="Batterie-Wechselrichter"
enabled=B"true"
id="batteryInverter0"
modbus.id="modbus1"
service.factoryPid="Battery-Inverter.Kaco.BlueplanetGridsave"
service.pid="Battery-Inverter.Kaco.BlueplanetGridsave.a42c2cec-2850-47f7-a1ae-fc4decdbb2c0"
startStop="AUTO"
EOL
}

# 
# Sinexcel battery inverter.
#
function setup_battery_inverter_sinexcel {
	echo "# Creating $CONF_BATTERY_INVERTER_SINEXCEL"
	mkdir -p "$(dirname $CONF_BATTERY_INVERTER_SINEXCEL)"
	cat >$CONF_BATTERY_INVERTER_SINEXCEL <<EOL
Modbus.target="(&(enabled\=true)(!(service.pid\=Battery-Inverter.Sinexcel.c543b730-87d6-4c18-9869-6976aec436c2))(|(id\=modbus1)))"
alias="Batterie-Wechselrichter"
enabled=B"true"
id="batteryInverter0"
modbus.id="modbus1"
service.factoryPid="Battery-Inverter.Sinexcel"
service.pid="Battery-Inverter.Sinexcel.c543b730-87d6-4c18-9869-6976aec436c2"
startStop="AUTO"
EOL
}

#
# ESS generic managed symmetric - ess0
#
function setup_ess0_generic_managed_symmetric {
	echo "# Creating $CONF_ESS_GENERIC_MANAGED_SYMMETRIC"
	mkdir -p "$(dirname $CONF_ESS_GENERIC_MANAGED_SYMMETRIC)"
	cat >$CONF_ESS_GENERIC_MANAGED_SYMMETRIC <<EOL
alias="Energiespeichersystem"
battery.id="bms0"
battery.target="(&(enabled\=true)(!(service.pid\=Ess.Generic.ManagedSymmetric.4b518bf7-5989-4c73-a0cb-ff6855fd4992))(|(id\=bms0)))"
batteryInverter.id="batteryInverter0"
batteryInverter.target="(&(enabled\=true)(!(service.pid\=Ess.Generic.ManagedSymmetric.4b518bf7-5989-4c73-a0cb-ff6855fd4992))(|(id\=batteryInverter0)))"
enabled=B"true"
id="ess0"
service.factoryPid="Ess.Generic.ManagedSymmetric"
service.pid="Ess.Generic.ManagedSymmetric.4b518bf7-5989-4c73-a0cb-ff6855fd4992"
startStop="START"
EOL
}

#
# ESS generic managed symmetric with battery0 - ess0
#
function setup_ess0_generic_managed_symmetric_with_battery0 {
	echo "# Creating $CONF_ESS_GENERIC_MANAGED_SYMMETRIC"
	mkdir -p "$(dirname $CONF_ESS_GENERIC_MANAGED_SYMMETRIC)"
	cat >$CONF_ESS_GENERIC_MANAGED_SYMMETRIC <<EOL
alias="Energiespeichersystem"
battery.id="battery0"
batteryInverter.id="batteryInverter0"
enabled=B"true"
id="ess0"
startStop="START"
service.factoryPid="Ess.Generic.ManagedSymmetric"
service.pid="Ess.Generic.ManagedSymmetric.4b518bf7-5989-4c73-a0cb-ff6855fd4992"
battery.target="(&(enabled\=true)(!(service.pid\=Ess.Generic.ManagedSymmetric.4b518bf7-5989-4c73-a0cb-ff6855fd4992))(|(id\=battery0)))"
batteryInverter.target="(&(enabled\=true)(!(service.pid\=Ess.Generic.ManagedSymmetric.4b518bf7-5989-4c73-a0cb-ff6855fd4992))(|(id\=batteryInverter0)))"
EOL
}

# Installed automatically with the newst Debian package (openems-core-fems 2021.9.2-2)
#
#function setup_beaglebone_usb_docking {
#	echo "# Start setup for 'BeagleBone mit USB-Docking'"
#	echo "# Creating /etc/systemd/network/enx.network"
#	cat >/etc/systemd/network/enx.network <<EOL
#[Match]
#Name=enx*
#
#[Network]
#DHCP=no
#Address=10.4.0.1/16
#Address=192.168.1.9/29
#EOL
#
#	echo "# Configuring /dev/busUSB1, /dev/busUSB2, /dev/busUSB3"
#	if [ -e /etc/udev/rules.d/99-usb-serial.rules ]; then
#		echo "# OVERWRITING /etc/udev/rules.d/99-usb-serial.rules!!!!"
#		echo "# before:"
#		cat /etc/udev/rules.d/99-usb-serial.rules
#		echo "#"
#	fi
#	cat >/etc/udev/rules.d/99-usb-serial.rules <<EOL
#SUBSYSTEM=="tty", ATTRS{idVendor}=="0403", ATTRS{idProduct}=="6001", KERNELS=="1-1.1", SYMLINK+="busUSB1"
#SUBSYSTEM=="tty", ATTRS{idVendor}=="0403", ATTRS{idProduct}=="6001", KERNELS=="1-1.2", SYMLINK+="busUSB2"
#SUBSYSTEM=="tty", ATTRS{idVendor}=="0403", ATTRS{idProduct}=="6001", KERNELS=="1-1.3", SYMLINK+="busUSB3"
#EOL
#
#	echo "# Applying UDEV rules"
#	udevadm control --reload-rules && udevadm trigger
#
#	echo "# Restarting systemd-networkd + systemd-resolved"
#	systemctl start systemd-networkd
#	systemctl enable systemd-networkd
#	systemctl start systemd-resolved
#	systemctl enable systemd-resolved
#	ln -sf /run/systemd/resolve/resolv.conf /etc/resolv.conf 
#	
#	echo "# Finished setup for 'BeagleBone mit USB-Docking'"
#	echo "# Reboot to make sure, the network is really activated'"
#	reboot
#}

function setup_pro_hybrid_10 {
	setup_generic
	setup_scheduler_readonly
	setup_pro_hybrid_10_components
}

function setup_mini {
	setup_generic
	setup_scheduler_readonly
	setup_modbus0_rtu
	setup_meter0_mini
	setup_ess0_mini
	setup_meter1_mini
}

function setup_dess {
	setup_generic
	setup_scheduler_readonly
	setup_modbus0_rtu
	setup_dess_components
}

function setup_pro_9_12 {
	setup_generic
	setup_scheduler_controlled
	setup_modbus0_rtu
	setup_meter0_socomec
	setup_ess0_pro912
	setup_meter1_pro912
	setup_ess_power_symmetric_mode
	setup_ctrl_limit_total_discharge
	setup_ctrl_balancing_symmetric
}

function setup_commercial_40_ac {
	setup_generic
	setup_scheduler_controlled
	setup_modbus0_rtu
	setup_modbus1_tcp_10_4_0_15
	setup_meter0_socomec
	setup_ess0_commercial_40
	setup_ctrl_limit_total_discharge
	setup_ctrl_balancing_symmetric
}

function setup_commercial_40_dc {
	setup_generic
	setup_scheduler_commercial_40_surplus
	setup_modbus0_rtu
	setup_modbus1_tcp_10_4_0_15
	setup_meter0_socomec
	setup_ess0_commercial_40
	setup_charger0_commercial_40
	setup_ctrl_limit_total_discharge
	setup_ctrl_balancing_symmetric
	setup_ctrl_commercial_40_surplus
}

function setup_commercial_50 {
	setup_generic
	setup_scheduler_controlled
	setup_modbus0_rtu
	setup_modbus1_tcp_10_4_0_10
	setup_meter0_socomec
	setup_bms0_soltaro_single_rack
	setup_ess0_kaco_blueplanet_gridsave_50
	setup_ctrl_limit_total_discharge
	setup_ctrl_balancing_symmetric
}


function setup_commercial_50_version_c_and_generic {
	setup_generic
	setup_scheduler_controlled
	setup_modbus0_rtu_bus0_57600
	setup_modbus1_tcp_10_4_0_10
	setup_modbus2_rtu_bus1_9600
	setup_meter0_socomec_modbus2
	setup_meter1_socomec_modbus2
	setup_bms0_soltaro_single_rack_version_c
	setup_battery_inverter_kaco_blueplanet_gridsave
	setup_ess0_generic_managed_symmetric
	setup_ctrl_limit_total_discharge_soltaro_version_c
	setup_ctrl_balancing_symmetric
}

# Docking - Modbus component using another Port
function setup_commercial_50_docking {
	
	setup_generic
	setup_scheduler_controlled
	setup_modbus0_rtu_docking_busUSB1
	setup_modbus1_tcp_10_4_0_10
	setup_meter0_socomec
	setup_bms0_soltaro_single_rack
	setup_ess0_kaco_blueplanet_gridsave_50
	setup_ctrl_limit_total_discharge
	setup_ctrl_balancing_symmetric
}

# Docking - Modbus component using another Port
function setup_commercial_50_version_c_and_generic_docking {
	setup_generic
	setup_scheduler_controlled
	setup_modbus0_rtu_bus0_57600_docking_busUSB1
	setup_modbus1_tcp_10_4_0_10
	setup_modbus2_rtu_bus1_9600_docking_busUSB2
	setup_meter0_socomec_modbus2
	setup_meter1_socomec_modbus2
	setup_bms0_soltaro_single_rack_version_c
	setup_battery_inverter_kaco_blueplanet_gridsave
	setup_ess0_generic_managed_symmetric
	setup_ctrl_limit_total_discharge_soltaro_version_c
	setup_ctrl_balancing_symmetric
}

function setup_commercial_30_gen_2 {
	setup_generic
	setup_scheduler_controlled_balancing
	setup_modbus0_rtu_techbase_AMA0
	setup_modbus1_tcp_192_168_1_11
	setup_modbus2_rtu_techbase_SC0
	setup_meter0_socomec_modbus2
	setup_meter1_socomec_modbus2
	setup_battery0_soltaro_gen2
	setup_battery_inverter_sinexcel
	setup_ess0_generic_managed_symmetric_with_battery0
	setup_ctrl_balancing_symmetric
	setup_relay_kmtronik_8port
}

function setup_commercial_30_version_c {
	setup_generic
	setup_scheduler_controlled
	setup_modbus0_rtu_bus0_57600
	setup_bms0_soltaro_single_rack_version_c_comm30
	setup_modbus1_tcp_192_168_1_11
	setup_battery_inverter_sinexcel
	setup_ess0_generic_managed_symmetric
	setup_modbus2_rtu_bus1_9600
	setup_meter0_socomec_modbus2
	setup_meter1_socomec_modbus2
	setup_ctrl_limit_total_discharge_soltaro_version_c
	setup_ctrl_balancing_symmetric
}

# Docking - Modbus component using another Port
function setup_commercial_30_version_c_docking {
	setup_generic
	setup_scheduler_controlled
	setup_modbus0_rtu_bus0_57600_docking_busUSB1
	setup_bms0_soltaro_single_rack_version_c_comm30
	setup_modbus1_tcp_192_168_1_11
	setup_battery_inverter_sinexcel
	setup_ess0_generic_managed_symmetric
	setup_modbus2_rtu_bus1_9600_docking_busUSB2
	setup_meter0_socomec_modbus2
	setup_meter1_socomec_modbus2
	setup_ctrl_limit_total_discharge_soltaro_version_c
	setup_ctrl_balancing_symmetric
}

# Offgrid & Docking - Modbus component using another Port
function setup_commercial_30_version_c_docking_offgrid {
	setup_generic
	setup_scheduler_controlled_emergency
	setup_modbus0_rtu_bus0_57600_docking_busUSB1
	setup_bms0_soltaro_single_rack_version_c_comm30_2021_17_1
	setup_modbus1_tcp_192_168_1_11
	setup_battery_inverter_sinexcel
	setup_modbus3_wago_offgrid_tcp_192_168_1_50
	setup_offgrid_io_wago
	setup_offgrid_offGridSwitch
	setup_ess0_generic_offgrid
	setup_modbus2_rtu_bus1_9600_docking_busUSB2
	setup_meter0_socomec_modbus2
	setup_meter1_socomec_modbus2
	setup_ctrl_balancing_symmetric
	setup_ctrl_emergency_capacity_reserve
}


function setup_pro_hybrid_10_gw {
	setup_generic
	setup_scheduler_readonly
	setup_modbus0_rtu
	setup_pro_hybrid_10_gw_components
}

function setup_kostal_piko {
	setup_generic
	setup_scheduler_readonly
	setup_kostal_piko_components
}

function setup_basis {
	setup_generic
	setup_scheduler_readonly
}

function setup_home {
	setup_generic
	setup_scheduler_hybrid_controlled
	setup_modbus0_rtu_usb_docking_busUSB1_baud_19200
	setup_modbus1_rtu_usb_docking_busUSB2_baud_9600
	setup_battery0_fenecon_home
	setup_fenecon_home_gw_components
	setup_ess0_generic_managed_symmetric_with_battery0
	setup_ctrl_surplus_feed_to_grid_limit
	setup_ctrl_balancing_symmetric
	setup_relay_kmtronik_4port
}


function not_implemented {
	echo "#"
	echo "# THIS NOT IMPLEMENTED!"
	echo "#"
}

function choose_setup {
	CHOICE=$(whiptail --menu "Select:" 20 60 12 \
		"Nur Basis-Setup" "" \
		"FENECON Commercial 30-Serie GEN_2" ""\
		"FENECON Pro Hybrid 10-Serie" "" \
		"FENECON Pro Hybrid 10 GW-Serie (GoodWe ET)" "" \
		"FENECON Commercial 50-Serie Version C mit docking" ""\
		"FENECON Commercial 30-Serie Version C mit docking" ""\
		"F-Commercial 30-Serie Version C mit docking Offgrid" ""\
		"FENECON Commercial 50-Serie Version C" ""\
		"FENECON Commercial 30-Serie Version C" ""\
		"FENECON HOME mit USB-Docking" ""\
		"FENECON Mini" "" \
		"FENECON DESS" "" \
		"FENECON Pro 9-12" "" \
		"FENECON Commercial 40 AC" "" \
		"FENECON Commercial 40 DC+Surplus" "" \
		"Commercial 50 Version B (Old) mit docking" "" \
		"Commercial 50 Version B (Old)" "" \
		"Kostal Piko" "" \
		3>&1 1>&2 2>&3)

	case $CHOICE in
		"Nur Basis-Setup")										setup_basis;;
		"FENECON Commercial 30-Serie GEN_2")					setup_commercial_30_gen_2;;
		"FENECON Pro Hybrid 10-Serie")							setup_pro_hybrid_10;;
		"FENECON Pro Hybrid 10 GW-Serie (GoodWe ET)")			setup_pro_hybrid_10_gw;;
		"FENECON Commercial 50-Serie Version C mit docking")	setup_commercial_50_version_c_and_generic_docking;;
		"FENECON Commercial 30-Serie Version C mit docking")	setup_commercial_30_version_c_docking;;
		"F-Commercial 30-Serie Version C mit docking Offgrid")	setup_commercial_30_version_c_docking_offgrid;;
		"FENECON Commercial 50-Serie Version C")				setup_commercial_50_version_c_and_generic;;
		"FENECON Commercial 30-Serie Version C")				setup_commercial_30_version_c;;
		"FENECON HOME mit USB-Docking")							setup_home;;
		"FENECON Mini")											setup_mini;;
		"FENECON DESS")											setup_dess;;
		"FENECON Pro 9-12")										setup_pro_9_12;;
		"FENECON Commercial 40 AC")								setup_commercial_40_ac;;
		"FENECON Commercial 40 DC+Surplus")						setup_commercial_40_dc;;
		"Commercial 50 Version B (Old) mit docking") 			setup_commercial_50_docking;;
		"Commercial 50 Version B (Old)")						setup_commercial_50;;
		"Kostal Piko")											setup_kostal_piko;;
	esac
}

# Start from calling parameter or show menu
case "$1" in
	pro_hybrid_10)						setup_pro_hybrid_10;;
	home)								setup_home;;
	basic)								setup_basis;;
	commercial_30_gen_2)				setup_commercial_30_gen_2;;
	commercial_50)						setup_commercial_50;;
	commercial_50_c)					setup_commercial_50_version_c_and_generic;;
	commercial_30_c)					setup_commercial_30_version_c;;
	commercial_50_docking)				setup_commercial_50_docking;;
	commercial_50_c_docking)			setup_commercial_50_version_c_and_generic_docking;;
	commercial_30_c_docking)			setup_commercial_30_version_c_docking;;
	commercial_30_c_docking_offgrid)	setup_commercial_30_version_c_docking_offgrid;;
	goodwe_et)							setup_pro_hybrid_10_gw;;
	mini)								setup_mini;;
	dess)								setup_dess;;
	pro_9_12)							setup_pro_9_12;;
	commercial_40_ac)					setup_commercial_40_ac;;
	setup_kostal_piko)					setup_kostal_piko;;
	*)									choose_setup;;
esac
