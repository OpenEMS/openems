package de.fenecon.femscore.modbus.device.ess;

import de.fenecon.femscore.modbus.device.ModbusDevice;

public abstract class Ess extends ModbusDevice {

	public Ess(String name, String modbusid, int unitid) {
		super(name, modbusid, unitid);
	}

	@Override
	public String toString() {
		return "ESS [name=" + name + ", unitid=" + unitid + "]";
	}
}
