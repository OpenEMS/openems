package io.openems.edge.bridge.modbus.api.facade;

import com.ghgande.j2mod.modbus.facade.ModbusTCPMaster;

public class MyModbusTCPMaster extends ModbusTCPMaster implements MyModbusMaster {

	private final static int DEFAULT_PORT = 502;
	private final static int DEFAULT_TIMEOUT = 10000;
	private final static boolean DEFAULT_RECONNECT = true;

	public MyModbusTCPMaster(String addr) {
		super(//
				addr, //
				DEFAULT_PORT, //
				DEFAULT_TIMEOUT, //
				DEFAULT_RECONNECT);
	}
}
