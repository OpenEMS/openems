# AC-Island Controller
## Description
This Controller connects the PV to the off-Grid connection of the ess, if an grid outtake happens.
To do so the Controller observes the GridMode of the ess.
If the Ess is OnGrid the PV will be connected to the Grid.
If the Ess is OffGrid and the Soc is below the MaxSoc parameter, the PV will be connected to the Load Connection of the ess. 
Is the Soc above the MaxSoc parameter, the PV will be disconnected until the Soc is below the MinSoc parameter.

## Parameter
* **maxSoc**: The Threshold to disconnect the PV in OffGrid mode
* **minSoc**: The Threshold to connect the PV in OffGrid mode
* **ess**: The ESS to get the Soc from
* **switchDelay**: The time to wait between disconnect OnGrid and connect OffGrid and vice versa
* **onGridOutputChannelAddress**: the DigitalOutput for the Relay to connect the PV to OnGrid
* **offGridOutputChannelAddress**: the DigitalOutput for the Relay to connect the PV to OffGrid