# REFU battery inverter ESS


Following Values are implemented:

|ChannelName|Unit|
|---|---|
|ActivePower|W|
|ActivePowerL1|W|
|ActivePowerL2|W|
|ActivePowerL3|W|
|alias||
|allowedApparent|VA|
|AllowedCharge|W|
|AllowedChargeCurrent|mA|
|AllowedDischarge|W|
|AllowedDischargeCurrent|mA|
|BatteryAlarm1|1: Normal charging over-current <br/>2: Charginig current over limit<br/>4: Discharging current over limit<br/>8: Normal high voltage<br/>16: Normal low voltage<br/>32: Abnormal voltage variation<br/>64: Normal high temperature<br/>128: Normal low temperature<br/>256: Abnormal temperature variation<br/>512: Serious high voltage<br/>1024: Serious low voltage<br/>2048: Serious low temperature<br/>4096: Charging serious over current<br/>8192: Discharging serious over current<br/>16384: Abnormal capacity alarm<br/>|
|BatteryAlarm2|1: EEPROM parameter failure<br/>2: Switch off inside combined cabinet<br/>32: Should not be connected to grid due to the DC side condition<br/>128: Emergency stop require from system controller<br/>|
|BatteryAlarm3|1: Battery group 1 enable and not connected to grid<br/>2: Battery group 2 enable and not connected to grid<br/>4: Battery group 3 enable and not connected to grid<br/>8: Battery group 4 enable and not connected to grid<br/>|
|BatteryAlarm4|1: The isolation switch of battery group 1 open<br/>2: The isolation switch of battery group 2 open<br/>4: The isolation switch of battery group 3 open<br/>8: The isolation switch of battery group 4 open<br/>|
|BatteryAlarm6|1: Balancing sampling failure of battery group 1<br/>2: Balancing sampling failure of battery group 2<br/>4: Balancing sampling failure of battery group 3<br/>8: Balancing sampling failure of battery group 4<br/>|
|BatteryAlarm7|1: Balancing control failure of battery group 1<br/>2: Balancing control failure of battery group 2<br/>4: Balancing control failure of battery group 3<br/>8: Balancing control failure of battery group 4<br/>|
|BatteryChargeEnergy|kWh|
|BatteryCurrent|mA|
|BatteryCurrentPcs|mA|
|BatteryDischargeEnergy|kWh|
|BatteryFault1|1: No enable batery group or usable battery group<br/>2: Normal leakage of battery group<br/>4: Serious leakage of battery group<br/>8: Battery start failure<br/>16: Battery stop failure<br/>32: Interruption of CAN Communication between battery group and controller<br/>1024: Emergency stop abnormal of auxiliary collector<br/>2048: Leakage self detection on negative<br/>4096: Leakage self detection on positive<br/>8192: Self detection failure on battery<br/>|
|BatteryFault10|4: Serious temperature fault<br/>8: Communication fault for system controller<br/>128: Frog alarm<br/>256: Fuse fault<br/>1024: Normal leakage<br/>2048: Serious leakage<br/>4096: CAN disconnection between battery group and battery stack<br/>8192: Central contactor circuit open<br/>16384: BMU power contactor open<br/>|
|BatteryFault11||
|BatteryFault12||
|BatteryFault13||
|BatteryFault14||
|BatteryFault2|1: CAN Communication interruption between battery group and group 1<br/>2: CAN Communication interruption between battery group and group 2<br/>4: CAN Communication interruption between battery group and group 3<br/>8: CAN Communication interruption between battery group and group 4<br/>|
|BatteryFault3|1: Main contractor abnormal in battery self detect group 1<br/>2: Main contractor abnormal in battery self detect group 2<br/>4: Main contractor abnormal in battery self detect group 3<br/>8: Main contractor abnormal in battery self detect group 4<br/>|
|BatteryFault4|1: Pre-charge contractor abnormal on battery self detect group 1<br/>2: Pre-charge contractor abnormal on battery self detect group 2<br/>4: Pre-charge contractor abnormal on battery self detect group 3<br/>8: Pre-charge contractor abnormal on battery self detect group 4<br/>|
|BatteryFault5|1: Main contact failure on battery control group 1<br/>2: Main contact failure on battery control group 2<br/>4: Main contact failure on battery control group 3<br/>8: Main contact failure on battery control group 4<br/>|
|BatteryFault6|1: Pre-charge failure on battery control group 1<br/>2: Pre-charge failure on battery control group 2<br/>4: Pre-charge failure on battery control group 3<br/>8: Pre-charge failure on battery control group 4<br/>|
|BatteryFault7||
|BatteryFault8||
|BatteryFault9|4: Sampling circuit abnormal for BMU<br/>8: Power cable disconnect failure<br/>16: Sampling circuit disconnect failure<br/>64: CAN disconnect for master and slave<br/>512: Sammpling circuit failure<br/>1024: Single battery failure<br/>2048: Circuit detection abnormal for main contactor<br/>4096: Circuit detection abnormal for main contactor<br/>8192: Circuit detection abnormal for Fancontactor<br/>16384: BMUPower contactor circuit detection abnormal<br/>32768: Central contactor circuit detection abnormal<br/>|
|BatteryGroupControlStatus||
|BatteryHighestTemperature|?C|
|BatteryHighestVoltage|mV|
|BatteryLowestTemperature|?C|
|BatteryLowestVoltage|mV|
|BatteryMode|0: Normal Mode<br/>|
|BatteryOperationStatus|1: Battery group 1 operating<br/>2: Battery group 2 operating<br/>4: Battery group 3 operating<br/>8: Battery group 4 operating<br/>|
|BatteryPower|W|
|BatteryState|0: Initial<br/>1: Stop<br/>2: Starting<br/>3: Start<br/>4: Stopping<br/>5: Fault<br/>|
|BatteryStopRequest||
|BatteryVoltage|mV|
|BatteryVoltagePcs|mV|
|capacity|Wh|
|chargeSoc||
|CommunicationInformations|1: Gateway Initialized<br/>2: Modbus Slave Status<br/>4: Modbus Master Status<br/>8: CAN Timeout<br/>16: First Communication Ok<br/>|
|CosPhi3p||
|CosPhiL1||
|CosPhiL2||
|CosPhiL3||
|Current|mA|
|CurrentL1|mA|
|CurrentL2|mA|
|CurrentL3|mA|
|DCDCError||
|DCDCStatus|1: Ready to Power on<br/>2: Ready for Operating<br/>4: Enabled<br/>8: DCDC Fault<br/>128: DCDC Warning<br/>256: Voltage/Current mode<br/>512: Power mode<br/>|
|ErrorCode||
|ErrorLog1||
|ErrorLog10||
|ErrorLog11||
|ErrorLog12||
|ErrorLog13||
|ErrorLog14||
|ErrorLog15||
|ErrorLog16||
|ErrorLog2||
|ErrorLog3||
|ErrorLog4||
|ErrorLog5||
|ErrorLog6||
|ErrorLog7||
|ErrorLog8||
|ErrorLog9||
|GridMode|1: On-Grid<br/>|
|InverterStatus|1: Ready to Power on<br/>2: Ready for Operating<br/>4: Enabled<br/>8: Fault<br/>256: Warning<br/>512: Voltage/Current mode<br/>1024: Power mode<br/>2048: AC relays close<br/>4096: DC relays 1 close<br/>8192: DC relays 2 close<br/>16384: Mains OK<br/>|
|maxNominalPower|VA|
|minSoc||
|PcsAllowedCharge|kW|
|PcsAllowedDischarge|kW|
|ReactivePower|Var|
|ReactivePowerL1|Var|
|ReactivePowerL2|Var|
|ReactivePowerL3|Var|
|SetActivePower|W|
|SetActivePowerL1|W|
|SetActivePowerL2|W|
|SetActivePowerL3|W|
|SetOperationMode|0: P/Q Set point<br/>1: IAC / cosphi set point<br/>|
|SetReactivePower|W|
|SetReactivePowerL1|W|
|SetReactivePowerL2|W|
|SetReactivePowerL3|W|
|SetSystemErrorReset|0: Off<br/>1: On<br/>|
|SetWorkState|0: Stop<br/>1: Start<br/>|
|Soc|%|
|SystemError1|1: BMS In Error<br/>2: BMS Overvoltage<br/>4: BMS Undervoltage<br/>8: BMS Overcurrent<br/>16: Error BMS Limits not initialized<br/>32: Connect Error<br/>64: Overvoltage warning<br/>128: Undervoltage warning<br/>256: Overcurrent warning<br/>512: BMS Ready<br/>1024: TREX Ready<br/>|
|SystemState|0: Stop<br/>1: Init<br/>2: Pre-operation<br/>3: Standby<br/>4: Start<br/>5: Fault<br/>|
|Warning||
