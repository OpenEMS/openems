package io.openems.edge.bridge.modbus.api.facade;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.procimg.Register;
import com.ghgande.j2mod.modbus.util.BitVector;

public interface MyModbusMaster {

	public void connect() throws Exception;

	public void disconnect();

	/**
	 * Read Holding Register, implementing Modbus function code 3
	 * 
	 * @param unitId
	 * @param ref
	 * @param count
	 * @return
	 * @throws ModbusException
	 */
	public Register[] readMultipleRegisters(int unitId, int ref, int count) throws ModbusException;

	/**
	 * Write Holding Registers, using Modbus function code 16
	 * 
	 * @param unitId
	 * @param ref
	 * @param registers
	 * @throws ModbusException
	 */
	public void writeMultipleRegisters(int unitId, int ref, Register[] registers) throws ModbusException;

	/**
	 * Read Coils, implementing Modbus function code 1
	 * 
	 * @param unitId
	 * @param ref
	 * @param count
	 * @return
	 * @throws ModbusException
	 */
	public BitVector readCoils(int unitId, int ref, int count) throws ModbusException;

	/**
	 * Write Single Coil, using Modbus function code 5
	 * 
	 * @param unitId
	 * @param ref
	 * @param state
	 * @return
	 * @throws ModbusException
	 */
	public boolean writeCoil(int unitId, int ref, boolean state) throws ModbusException;
}
