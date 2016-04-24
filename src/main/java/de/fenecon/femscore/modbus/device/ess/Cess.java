package de.fenecon.femscore.modbus.device.ess;

import java.util.List;

import de.fenecon.femscore.modbus.device.ModbusDeviceWritable;

public class Cess extends Ess implements ModbusDeviceWritable {

	public Cess(String name, String modbusid, int unitid) {
		super(name, modbusid, unitid);
	}
	
	@Override
	public void executeModbusWrite() {
		// TODO Auto-generated method stub	
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
		return "CESS [name=" + name + ", unitid=" + unitid + "]";
	}

}
