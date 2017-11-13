# FENECON Pro ESS


Following Values are implemented:

|ChannelName|Unit|
|---|---|
|ActivePowerL1|W|
|ActivePowerL2|W|
|ActivePowerL3|W|
|AllowedApparent||
|AllowedCharge|W|
|AllowedDischarge|W|
|BatteryCurrent|mA|
|BatteryGroupAlarm|1: Fail, The system should be stopped<br/>2: Common low voltage alarm<br/>4: Common high voltage alarm<br/>8: Charging over current alarm<br/>16: Discharging over current alarm<br/>32: Over temperature alarm<br/>64: Interal communication abnormal<br/>|
|BatteryGroupState|0: Initial<br/>1: Stop<br/>2: Starting<br/>3: Running<br/>4: Stopping<br/>5: Fail<br/>|
|BatteryPower|W|
|BatteryTemperatureSection1|°C|
|BatteryTemperatureSection10|°C|
|BatteryTemperatureSection11|°C|
|BatteryTemperatureSection12|°C|
|BatteryTemperatureSection13|°C|
|BatteryTemperatureSection14|°C|
|BatteryTemperatureSection15|°C|
|BatteryTemperatureSection16|°C|
|BatteryTemperatureSection2|°C|
|BatteryTemperatureSection3|°C|
|BatteryTemperatureSection4|°C|
|BatteryTemperatureSection5|°C|
|BatteryTemperatureSection6|°C|
|BatteryTemperatureSection7|°C|
|BatteryTemperatureSection8|°C|
|BatteryTemperatureSection9|°C|
|BatteryVoltage|mV|
|BatteryVoltageSection1|mV|
|BatteryVoltageSection10|mV|
|BatteryVoltageSection11|mV|
|BatteryVoltageSection12|mV|
|BatteryVoltageSection13|mV|
|BatteryVoltageSection14|mV|
|BatteryVoltageSection15|mV|
|BatteryVoltageSection16|mV|
|BatteryVoltageSection2|mV|
|BatteryVoltageSection3|mV|
|BatteryVoltageSection4|mV|
|BatteryVoltageSection5|mV|
|BatteryVoltageSection6|mV|
|BatteryVoltageSection7|mV|
|BatteryVoltageSection8|mV|
|BatteryVoltageSection9|mV|
|capacity|Wh|
|chargeSoc||
|ControlMode|1: Remote<br/>2: Local<br/>|
|CurrentL1|mA|
|CurrentL2|mA|
|CurrentL3|mA|
|FrequencyL1|mHz|
|FrequencyL2|mHz|
|FrequencyL3|mHz|
|GridMode|0: Off-Grid<br/>1: On-Grid<br/>|
|maxNominalPower|VA|
|minSoc||
|NativeSetActivePowerL1|W|
|NativeSetActivePowerL2|W|
|NativeSetActivePowerL3|W|
|NativeSetReactivePowerL1|Var|
|NativeSetReactivePowerL2|Var|
|SetReactivePowerL3|Var|
|PcsAlarm1L1|1: Grid undervoltage<br/>2: Grid overvoltage<br/>4: Grid under frequency<br/>8: Grid over frequency<br/>16: Grid power supply off<br/>32: Grid condition unmeet<br/>64: DC under voltage<br/>128: Input over resistance<br/>256: Combination error<br/>512: Comm with inverter error<br/>1024: Tme error<br/>|
|PcsAlarm1L2|1: Grid undervoltage<br/>2: Grid overvoltage<br/>4: Grid under frequency<br/>8: Grid over frequency<br/>16: Grid power supply off<br/>32: Grid condition unmeet<br/>64: DC under voltage<br/>128: Input over resistance<br/>256: Combination error<br/>512: Comm with inverter error<br/>1024: Tme error<br/>|
|PcsAlarm1L3|1: Grid undervoltage<br/>2: Grid overvoltage<br/>4: Grid under frequency<br/>8: Grid over frequency<br/>16: Grid power supply off<br/>32: Grid condition unmeet<br/>64: DC under voltage<br/>128: Input over resistance<br/>256: Combination error<br/>512: Comm with inverter error<br/>1024: Tme error<br/>|
|PcsAlarm2L1||
|PcsAlarm2L2||
|PcsAlarm2L3||
|PcsFault1L1|1: Control current overload 100%<br/>2: Control current overload 110%<br/>4: Control current overload 150%<br/>8: Control current overload 200%<br/>16: Control current overload 120%<br/>32: Control current overload 300%<br/>64: Control transient load 300%<br/>128: Grid over current<br/>256: Locking waveform too many times<br/>512: Inverter voltage zero drift error<br/>1024: Grid voltage zero drift error<br/>2048: Control current zero drift error<br/>4096: Inverter current zero drift error<br/>8192: Grid current zero drift error<br/>16384: PDP protection<br/>32768: Hardware control current protection<br/>|
|PcsFault1L2|1: Control current overload 100%<br/>2: Control current overload 110%<br/>4: Control current overload 150%<br/>8: Control current overload 200%<br/>16: Control current overload 120%<br/>32: Control current overload 300%<br/>64: Control transient load 300%<br/>128: Grid over current<br/>256: Locking waveform too many times<br/>512: Inverter voltage zero drift error<br/>1024: Grid voltage zero drift error<br/>2048: Control current zero drift error<br/>4096: Inverter current zero drift error<br/>8192: Grid current zero drift error<br/>16384: PDP protection<br/>32768: Hardware control current protection<br/>|
|PcsFault1L3|1: Control current overload 100%<br/>2: Control current overload 110%<br/>4: Control current overload 150%<br/>8: Control current overload 200%<br/>16: Control current overload 120%<br/>32: Control current overload 300%<br/>64: Control transient load 300%<br/>128: Grid over current<br/>256: Locking waveform too many times<br/>512: Inverter voltage zero drift error<br/>1024: Grid voltage zero drift error<br/>2048: Control current zero drift error<br/>4096: Inverter current zero drift error<br/>8192: Grid current zero drift error<br/>16384: PDP protection<br/>32768: Hardware control current protection<br/>|
|PcsFault2L1|1: Hardware AC volt. protection<br/>2: Hardware DC curr. protection<br/>4: Hardware temperature protection<br/>8: No capturing signal<br/>16: DC overvoltage<br/>32: DC disconnected<br/>64: Inverter undervoltage<br/>128: Inverter overvoltage<br/>256: Current sensor fail<br/>512: Voltage sensor fail<br/>1024: Power uncontrollable<br/>2048: Current uncontrollable<br/>4096: Fan error<br/>8192: Phase lack<br/>16384: Inverter relay fault<br/>32768: Grid relay fault<br/>|
|PcsFault2L2|1: Hardware AC volt. protection<br/>2: Hardware DC curr. protection<br/>4: Hardware temperature protection<br/>8: No capturing signal<br/>16: DC overvoltage<br/>32: DC disconnected<br/>64: Inverter undervoltage<br/>128: Inverter overvoltage<br/>256: Current sensor fail<br/>512: Voltage sensor fail<br/>1024: Power uncontrollable<br/>2048: Current uncontrollable<br/>4096: Fan error<br/>8192: Phase lack<br/>16384: Inverter relay fault<br/>32768: Grid relay fault<br/>|
|PcsFault2L3|1: Hardware AC volt. protection<br/>2: Hardware DC curr. protection<br/>4: Hardware temperature protection<br/>8: No capturing signal<br/>16: DC overvoltage<br/>32: DC disconnected<br/>64: Inverter undervoltage<br/>128: Inverter overvoltage<br/>256: Current sensor fail<br/>512: Voltage sensor fail<br/>1024: Power uncontrollable<br/>2048: Current uncontrollable<br/>4096: Fan error<br/>8192: Phase lack<br/>16384: Inverter relay fault<br/>32768: Grid relay fault<br/>|
|PcsFault3L1|1: Control panel overtemp<br/>2: Power panel overtemp<br/>4: DC input overcurrent<br/>8: Capacitor overtemp<br/>16: Radiator overtemp<br/>32: Transformer overtemp<br/>64: Combination comm error<br/>128: EEPROM error<br/>256: Load current zero drift error<br/>512: Current limit-R error<br/>1024: Phase sync error<br/>2048: External PV current zero drift error<br/>4096: External grid current zero drift error<br/>|
|PcsFault3L2|1: Control panel overtemp<br/>2: Power panel overtemp<br/>4: DC input overcurrent<br/>8: Capacitor overtemp<br/>16: Radiator overtemp<br/>32: Transformer overtemp<br/>64: Combination comm error<br/>128: EEPROM error<br/>256: Load current zero drift error<br/>512: Current limit-R error<br/>1024: Phase sync error<br/>2048: External PV current zero drift error<br/>4096: External grid current zero drift error<br/>|
|PcsFault3L3|1: Control panel overtemp<br/>2: Power panel overtemp<br/>4: DC input overcurrent<br/>8: Capacitor overtemp<br/>16: Radiator overtemp<br/>32: Transformer overtemp<br/>64: Combination comm error<br/>128: EEPROM error<br/>256: Load current zero drift error<br/>512: Current limit-R error<br/>1024: Phase sync error<br/>2048: External PV current zero drift error<br/>4096: External grid current zero drift error<br/>|
|PcsMode|0: Emergency<br/>1: ConsumersPeakPattern<br/>2: Economic<br/>3: Eco<br/>4: Debug<br/>5: SmoothPv<br/>6: Remote<br/>|
|PcsOperationState|0: Self-checking<br/>1: Standby<br/>2: Off grid PV<br/>3: Off grid<br/>4: On-Grid<br/>5: Fail<br/>6: bypass 1<br/>7: bypass 2<br/>|
|PhaseAllowedApparentPower|VA|
|ReactivePowerL1|var|
|ReactivePowerL2|var|
|ReactivePowerL3|var|
|Day||
|Hour||
|Minute||
|Month||
|Second||
|Year||
|SetActivePowerL1||
|SetActivePowerL2||
|SetActivePowerL3||
|SetPcsMode|0: Emergency<br/>1: ConsumersPeakPattern<br/>2: Economic<br/>3: Eco<br/>4: Debug<br/>5: SmoothPv<br/>6: Remote<br/>|
|SetReactivePowerL1||
|SetReactivePowerL2||
|SetReactivePowerL3||
|SetSetupMode|0: Off<br/>1: On<br/>|
|SetWorkState|0: Local control<br/>1: Start<br/>2: Remote control off grid starting<br/>3: Stop<br/>4: Emergency Stop<br/>|
|SetupMode|0: Off<br/>1: On<br/>|
|Soc|%|
|SystemState|0: Standby<br/>1: Start Off-Grid<br/>2: Start<br/>3: Fault<br/>4: Off-grid PV<br/>|
|TotalBatteryChargeEnergy|Wh|
|TotalBatteryDischargeEnergy|Wh|
|VoltageL1|mV|
|VoltageL2|mV|
|VoltageL3|mV|
|Warning||
|WorkMode|2: Economy<br/>6: Remote<br/>8: Timing<br/>|
# FENECON Pro Meter


Following Values are implemented:

|ChannelName|Unit|
|---|---|
|ActiveEnergyL1|Wh|
|ActiveEnergyL2|Wh|
|ActiveEnergyL3|Wh|
|ActivePower||
|ActivePowerL1||
|ActivePowerL2||
|ActivePowerL3||
|apparentPower||
|currentL1||
|currentL2||
|currentL3||
|frequency||
|maxActivePower||
|minActivePower||
|ReactivePower||
|ReactivePowerL1||
|ReactivePowerL2||
|ReactivePowerL3||
|type||
|VoltageL1|mV|
|VoltageL1|mV|
|VoltageL2|mV|
|VoltageL3|mV|
