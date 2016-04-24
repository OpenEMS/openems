package de.fenecon.femscore.modbus.device;

import de.fenecon.femscore.modbus.ModbusWorker;
import net.wimpi.modbus.procimg.Register;

public abstract class ModbusDevice {

	protected final Integer unitid;
	protected final String modbusId;
	protected final String name;
	//protected final 
	
	public ModbusDevice(String name, String modbusid, int unitid) {
		this.unitid = unitid;
		this.name = name;
		this.modbusId = modbusid;
	}
	
	public String getModbusid() {
		return modbusId;
	}
	
	public String getName() {
		return name;
	}
	
	public abstract void executeModbusMainQuery();
	
	public abstract void executeModbusNextSmallQuery();
	
	protected Register[] getModbusResponse(ModbusWorker worker, int ref, int count) throws Exception {
		return worker.getModbusResponse(unitid, ref, count);
	}
	
	protected void writeRegister(ModbusWorker worker, int ref, Register reg) throws Exception {
		worker.writeRegister(unitid, ref, reg);
	}

	@Override
	public String toString() {
		return "ModbusDevice [name=" + name + ", unitid=" + unitid + "]";
	}
}
