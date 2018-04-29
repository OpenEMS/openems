package io.openems.edge.bridge.modbus.facade;

import com.ghgande.j2mod.modbus.facade.ModbusTCPMaster;

public class MyModbusTCPMaster extends ModbusTCPMaster implements MyModbusMaster {

	public MyModbusTCPMaster(String addr) {
		super(addr);
	}

	public MyModbusTCPMaster(String addr, int port, int timeout, boolean reconnect) {
		super(addr, port, timeout, reconnect);
	}
}
