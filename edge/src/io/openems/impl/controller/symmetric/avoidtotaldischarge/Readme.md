# AvoidTotalDischargeController
## Description
This Controller should improve the battery health. This is seperated in three functions:
1. Avoid a discharge of the Battery below an certain Soc level to reserve capacity for GridOuttakes. Thatfore the Controller set the max value for setActivePower to zero. So no Controller with lower Priority can discharge the ess anymore. If the Soc gets below the ChargeSoc the Controller sets the max value for SetActivePower to 1/5 of the min value of SetActivePower, if present otherwise to 1000W.
2. Discharge battery once per month completely. Some ESS needs a complete discharge once per month to reset the Soc calculation.
3. Block Charge if Battery is full. If the Battery is Charged completely and allowedCharge value of the ESS is Zero, the controller set the min value for setActivePower to Zero until the Soc is below the maxSoc. This is done because the BatteryCells lower thei voltage after full charge and the allowedCharge would allow further charge for the Battery, but this is unhealty for the Battery, so we block the charge.

## Parameter
* **esss**: the ESS which should be protected by the Controller. the ess also contains the minSoc and chargeSoc
* **maxSoc**: the soc to block the charge after the Battery was full.
