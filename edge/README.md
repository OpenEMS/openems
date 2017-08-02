# OpenEMS - Open Source Energy Management System

## Architecture

![Architecture](../doc/architecture.jpg)

-	Hardware (battery systems, inverters, meters,…) or external services are at the bottom of the graph.
-	“Bridges” are the physical connection layer to a device (like Modbus/TCP or RTU). There can be several devices per Bridge.
-	“Devices” are representing an external hardware or service.
-	A device can have several “DeviceNatures”. For example a FENECON Pro storage system is integrating a battery as well as a meter.
-	A nature has a “Channel” for each single information, which can be readonly (State of charge of a battery, current active power,…) and writable (set active power, switch of relay output)
-	Central interface is the “Databus”, where all events (like changed values of a channels) are registered and commands are forwarded.
-	The actual business logic is in the “Controllers”, where each of them holds specific, encapsulated tasks:
  -	“Controllers are Apps that are logically binding the Channels and the functionality.” For example: switching a relay output, charging the battery,…
  -	“AvoidTotalDischargeController” takes care of the secure function of a storage system
  -	“BalancingController” optimizes the self-consumption at the grid meter
  -	“ApiController” receives commands from external control units
-	The “Scheduler” is planning and executing the controllers according to the defined, dynamic priorities.
-	The configuration sits in a central JSON config file.


## Contact

For more information or support, please contact us at [fems@fenecon.de](mailto:fems@fenecon.de).
