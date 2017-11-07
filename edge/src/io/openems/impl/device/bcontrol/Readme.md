# B-Control Energy Meter
This is the Modbus implementation for the [B-Control Energy Meter](https://www.b-control.com/produkte/em-300.html).

Following Values are implemented:
|Modbusaddress|Channelname|Unit|
|---|---|---|
|0|ActivePowerPos|W|
|2|ActivePowerNeg|W|
|4|ReactivePowerPos|Var|
|6|ReactivePowerNeg|Var|
|16|ApparentPowerPos|VA|
|18|ApparentPowerNeg|VA|
|26|Frequency|mHZ|
|40|ActivePowerL1Pos|W|
|42|ActivePowerL1Neg|W|
|44|ReactivePowerL1Pos|Var|
|46|ReactivePowerL1Neg|Var|
|60|CurrentL1|mA|
|62|VoltageL1|mV|
|80|ActivePowerL2Pos|W|
|82|ActivePowerL2Neg|W|
|84|ReactivePowerL2Pos|Var|
|86|ReactivePowerL2Neg|Var|
|100|CurrentL2|mA|
|102|VoltageL2|mV|
|120|ActivePowerL2Pos|W|
|122|ActivePowerL2Neg|W|
|124|ReactivePowerL2Pos|Var|
|126|ReactivePowerL2Neg|Var|
|140|CurrentL2|mA|
|142|VoltageL2|mV|
|Calculated|ActivePower|W|
|Calculated|ActivePowerL1|W|
|Calculated|ActivePowerL2|W|
|Calculated|ActivePowerL3|W|
|Calculated|ReactivePower|Var|
|Calculated|ReactivePowerL1|Var|
|Calculated|ReactivePowerL2|Var|
|Calculated|ReactivePowerL3|Var|
|Calculated|ApparentPower|Var|