package de.fenecon.femscore.modbus.device.counter;

import de.fenecon.femscore.modbus.device.ModbusDevice;

public abstract class Counter extends ModbusDevice {

	protected boolean inverted = false;

	public Counter(String name, String modbusid, int unitid) {
		super(name, modbusid, unitid);
	}

	public Counter(String name, String modbusid, int unitid, boolean isValueInverted) {
		super(name, modbusid, unitid);
		this.inverted = isValueInverted;
	}

	@Override
	public String toString() {
		return "Counter [name=" + name + ", unitid=" + unitid + "]";
	}

	public void setInverted(boolean inverted) {
		this.inverted = inverted;
	}
}
