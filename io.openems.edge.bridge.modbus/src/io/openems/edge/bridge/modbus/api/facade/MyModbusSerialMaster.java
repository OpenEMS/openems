package io.openems.edge.bridge.modbus.api.facade;

import com.ghgande.j2mod.modbus.facade.ModbusSerialMaster;
import com.ghgande.j2mod.modbus.util.SerialParameters;

public class MyModbusSerialMaster extends ModbusSerialMaster implements MyModbusMaster {

	public MyModbusSerialMaster(SerialParameters params) {
		super(params);
	}

}
