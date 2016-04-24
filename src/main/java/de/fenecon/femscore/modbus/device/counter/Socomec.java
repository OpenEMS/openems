package de.fenecon.femscore.modbus.device.counter;

public class Socomec extends Counter {

	public Socomec(String name, String modbusid, int unitid) {
		super(name, modbusid, unitid);
	}

	@Override
	public void executeModbusMainQuery() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void executeModbusNextSmallQuery() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String toString() {
		return "Socomec [name=" + name + ", unitid=" + unitid + "]";
	}
}
