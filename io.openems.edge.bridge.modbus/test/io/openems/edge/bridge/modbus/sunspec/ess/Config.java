package io.openems.edge.bridge.modbus.sunspec.ess;

@interface Config {
	String id();

	String alias();

	boolean enabled();

	boolean readOnly();

	String modbus_id();

	int modbusUnitId();

	int readFromModbusBlock();

	String Modbus_target();
}
