# BYD Industrial Energy Storage System
This is the Modbus implementation for the BYD BEM125ktla01 ESS.

Following Values are implemented:
|Modbusaddress|Channelname|Unit|
|---|---|---|
|256|SysAlarmInfo|StatusField<br/> 0: Warning State<br/> 1: Protection State<br/> 2: Derating State<br/> 4: Charge Forbidden<br/> 16: Discharge Forbidden|
|257|SysWorkStatus|StatusField<br/> 0: Initial<br/>1: Fault</br>2: Stop<br/>4: Hot Standby<br/>8: Monitoring<br/>16: Standby<br/>32: Operation<br/>64: Debug|
|258|SysControlMode|StatusField<br/>0: Remote<br/>1: Local|
|272|SysAlarmInfo|StatusField<br/>0: Status abnormal of AC surge protector<br/>1: Close of control switch<br/>2: Emergency Stop<br/>4: Status abnormal of frog detector<br/>8: Serious leakage<br/>16: Normal leakage|
|273|SysAlarmInfo2|StatusField<br/>0: Failure of temperature sensor in control cabinet<br/>1: Close of control switch|
|4864|BatteryStackVoltage|mV|
|4865|BatteryStackCurrent|mA|
|4866|BatteryStackPower|W|
|4867|BatteryStackSoc|%|
|4868|BatteryStackSoh|%|
|4869|BatteryStackMaxChargeCurrent|mA|
|4870|BatteryStackMaxDischargeCurrent|mA|
|4871|BatteryStackMaxChargePower|W|
|4872|BatteryStackMaxDischargePower|W|
|4873|BatteryStackTotalCapacity|Wh|
|4874|BatteryStackTotalCharge|kWh|
|4875|BatteryStackTotalDischarge|kWh|