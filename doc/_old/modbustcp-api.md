# OpenEMS Modbus/TCP-Api

This describes the OpenEMS Modbus/TCP client implementation. For a working example see [openems-modbus-master](https://github.com/OpenEMS/openems-modbus-master).

## Setup Modbus/TCP-Controller

OpenEMS Modbus/TCP-Api is implemented as a Controller. To be active it needs to be activated in the Scheduler. Default port is 502.

Example configuration:
```
{
  "class": "io.openems.impl.controller.api.modbustcp.ModbusTcpApiController",
  "priority": 150,
  ["port": 502]
  "mapping": {
    "0": "system0/OpenemsVersionMajor"
  }
}
```

With the "mapping" config option, OpenEMS channels can be mapped to Modbus/TCP register addresses.

Note: Only channels with fixed bit length - like Integer, Long, Boolean - can be mapped to Modbus. See [implementation](https://github.com/OpenEMS/openems/blob/master/edge/src/io/openems/core/utilities/BitUtils.java#L42). 

## Access Modbus/TCP-Api

The following simple Java-Application prints - based on the example above - the OpenEMS major version number - using "[com.ghgande.j2mod](https://github.com/steveohara/j2mod)"-implementation

```
ModbusTCPMaster master = new ModbusTCPMaster("localhost", 502);
master.connect();
InputRegister[] r = master.readInputRegisters(0, 2);
System.out.println(r[0]);
System.out.println(r[1]);
```

Note: Only modbus function code 04 "Read Input Registers" is implemented for now, so there is no write functionality available yet.
