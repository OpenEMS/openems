package de.fenecon.femscore.modbus.device.counter;

import de.fenecon.femscore.modbus.device.ModbusDevice;

public abstract class Counter extends ModbusDevice {

	public Counter(String name, String modbusid, int unitid) {
		super(name, modbusid, unitid);
	}

	@Override
	public String toString() {
		return "Counter [name=" + name + ", unitid=" + unitid + "]";
	}

}
