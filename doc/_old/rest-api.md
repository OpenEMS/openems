# OpenEMS REST-Api

## Setup REST-Api-Controller

OpenEMS Rest-Api is implemented as a Controller. To be active it needs to be activated in the Scheduler. Default port is 8084.

Example configuration:
```
{
  "class": "io.openems.impl.controller.api.rest.RestApiController",
  "priority": 150,
  ["port": 8084]
}
```    


## Channel

Every "Channel" in OpenEMS can be accessed via REST-Api.

To connect to the OpenEMS REST-Api you need a URL with following pattern:

`http://x:<PASSWORD>@<IP>:8084/rest/channel/<ThingId>/<ChannelName>`

* To read values you need the to use GET.
* For write commands you have to use POST. Write commands are only possible on ConfigChannels.
* ThingId: The id of the Controller/Device which contains the Channel you want to read/write.
* ChannelName: the channel address to read/write.

### Read Channel value
Example: read State-of-charge (of first connected battery system/inverter named "ess0"):
```
http://x:<PASSWORD>@<IP>:8084/rest/channel/ess0/Soc
```

Result:
```
{
  "value": 55,
  "type": "ModbusReadLongChannel",
  "writeable": false
}
```

### Write Channel value
Example: set active-power of ess0
1. You need to activate the FixValueController:
```
   {
    "id": "FixValue",
	"class": "io.openems.impl.controller.asymmetric.fixvalue.FixValueActivePowerController",
	"esss": [
		"ess0"
	],
	"priority": 150,
	"activePowerL1": 0,
	"activePowerL2": 0,
	"activePowerL3": 0
    }
```
2. Set the "activePowerL1" ConfigChannel to 1000W(Discharge)
```
http://x:<PASSWORD>@<IP>:8084/rest/channel/FixValue/activePowerL1
Content-Type: application/json
Method: POST
Body: {"value":1000}
```
