# BallancingController
## Description
This controller calculates the Active and Reactive power for the defined ESS, to use as much pv energy as possible by your own.
Therefore the controller adds the actual ActivePower of all ESS and the actual ActivePower of the GridMeter. The Result is the new Power the ESS has to produce.
If there is more than one ESS the power is split and weight by the UseableSoc(=Soc-minSoc).
Afterwards the ReactivePower will be calculated with the cosPhi(0-1.0) according to the ActivePower set per ESS.

## Parameter
* **esss**: a array of ESS to use
* **meter**: the meter to use for the selfconsumption calculations
* **cosPhi**: the cosPhi angel to set the ReactivePower according to the ActivePower