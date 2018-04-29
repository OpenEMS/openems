package io.openems.edge.bridge.modbus.facade;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.procimg.Register;

public interface MyModbusMaster {

	public void connect() throws Exception;

	public void disconnect();

	public Register[] readMultipleRegisters(int unitId, int ref, int count) throws ModbusException;

	public void writeMultipleRegisters(int unitId, int ref, Register[] registers) throws ModbusException;

}
