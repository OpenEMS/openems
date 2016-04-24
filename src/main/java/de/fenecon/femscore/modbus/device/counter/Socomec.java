package de.fenecon.femscore.modbus.device.counter;

import java.util.List;

import de.fenecon.femscore.modbus.protocol.ElementRange;

public class Socomec extends Counter {

	public Socomec(String name, String modbusid, int unitid) {
		super(name, modbusid, unitid);
	}

	@Override
	public String toString() {
		return "Socomec [name=" + name + ", unitid=" + unitid + "]";
	}

	@Override
	protected List<ElementRange> getMainProtocol() {
		// TODO Auto-generated method stub
		return null;
	}
}
