# io.openems.edge.controller.valve Provider

This Bundle Provides ValveController. ATM There is only One Controller.
The Controller is a ValveControllerStaticPosition, handling the Valveposition by it's current Temperature.

## Example
You Configure the ValveController with a temperature:Position Map
e.g.:
200 dc == 10% Valve

400dc == 30% Valve
370dc == 25% Valve
700 dc == 100% Valve

Now a Calling Component can set the Temperature of the ValveController so it handles itself
E.g. Decentral Heater says "Thermometer is atm 350dc"
Now the ValveController will go through the configuration list and checks for the closest temperature that is at least met
--> 200dc < 350dc beneath temperature but will be got first; then 400 dc > 350dc >check i take that
370 dc > 350 dc && 370 < 400 dc check i take that
Output -> 25% Valve
## References

