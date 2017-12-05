# BalancingBandgap
## Description
This Controller calculates the ActivePower or ReactivePower by add the ESS Active-/ReactivePower and Meter Active-/ReactivePower.
If the Power is above the maxActive-/ReactivePower, the calculated Active-/ReactivePower - maxActive-/ReactivePower, is the new power for the ESS.
Is the Power below the minActive-/ReactivePower, the calculated Active-/ReactivePower - maxActive-/ReactivePower, is the new power for the ESS.
There are two seperate Controllers for Active and Reactive power.

## Parameter
* **esss**: the ESS to control
* **meter**: the Meter to use for the calculation
* **maxActivePowerL1/maxActivePowerL2/maxActivePowerL3 or maxReactivePowerL1/maxReactivePowerL2/maxReactivePowerL3**: the max value per Phase. if the Power consumption on the meter is greater, the ess will discharge the difference.
* **minActivePowerL1/minActivePowerL2/minActivePowerL3 or minReactivePowerL1/minReactivePowerL2/minReactivePowerL3**: the min value per Phase. if the Power consumption on the meter is smaller, the ess will charge the difference.