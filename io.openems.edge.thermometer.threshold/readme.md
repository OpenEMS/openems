# io.openems.edge.temperature.threshold Provider

A Threshold Thermometer getting a "Real" Temperature and set the Temperature by
discrete Values (Depending on Threshold Value)
Has a SetPoint and Thermometer state and calling components can check, if their setPointTemperature/given Temperature
is above/below the "fictional" Temperature depending on Thermometer state.

## Example
Threshold = 10dc
Real temperature = 543dc
"Fictional" Temperature becomes 540 dC
if Temperature becomes 551 --> Fictional will be 550dc and "Rising"
if State is Rising --> and Temperaturecheck --> 550dc > fictional Temperature --> return true (because temperature rises)
else if state is falling and TemperatureCheck 550dc > fictional Temperature --> return false
## References

