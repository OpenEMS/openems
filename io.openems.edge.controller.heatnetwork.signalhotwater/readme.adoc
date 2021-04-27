This is the Consolinno SignalHotWater Controller. It monitors two temperature sensors on a water tank and listens 
to a remote signal about the heat network status. 
* The water tank upper sensor monitors the minimum temperature, the water tank lower sensor monitors the maximum temperature.
* When the temperature falls below the minimum, the controller sets the heatTankRequest channel to "true", which signals 
  to a remote station to activate the heat network.
* The controller then waits for a response from the remote station that the heat network is ready ("true" is written in 
  the heatNetworkReadySignal channel by the remote station), or proceeds after a timeout. Then the needHotWater channel 
  is set to "true", which signals to the next controller to start transferring heat from the heat network to the water tank.
* While the water tank temperature is within bounds, changes (!) in the heatNetworkReadySignal channel are passed on to 
  the needHotWater channel. That means, if the heatNetworkReadySignal channel changes from "true" to "false" while the 
  water tank is heating up and is already above min temperature, needHotWater will change to "false" and the heating will 
  stop. If heatNetworkReadySignal changes from "false" to "true" while the tank is below max temperature, needHotWater 
  will change to "true" and heating will start.
