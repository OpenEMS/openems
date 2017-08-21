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

Example: read State-of-charge (of first connected battery system/inverter named "ess0"):
```
http://x:<PASSWORD>@<FEMS-IP>:8084/rest/channel/ess0/Soc
```

Result:
```
{
  "value": 55,
  "type": "ModbusReadLongChannel",
  "writeable": false
}
```
