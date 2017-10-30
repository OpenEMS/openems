/*******************************************************************************
 * OpenEMS - Open Source Energy Management System
 * Copyright (c) 2016, 2017 FENECON GmbH and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * Contributors:
 *   FENECON GmbH - initial API and implementation and initial documentation
 *******************************************************************************/
package io.openems.impl.protocol.modbus;

import java.util.BitSet;
import java.util.List;
import java.util.StringJoiner;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.io.ModbusTransaction;
import com.ghgande.j2mod.modbus.msg.ExceptionResponse;
import com.ghgande.j2mod.modbus.msg.ModbusResponse;
import com.ghgande.j2mod.modbus.msg.ReadCoilsRequest;
import com.ghgande.j2mod.modbus.msg.ReadCoilsResponse;
import com.ghgande.j2mod.modbus.msg.ReadInputRegistersRequest;
import com.ghgande.j2mod.modbus.msg.ReadInputRegistersResponse;
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersRequest;
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersResponse;
import com.ghgande.j2mod.modbus.msg.WriteCoilRequest;
import com.ghgande.j2mod.modbus.msg.WriteMultipleCoilsRequest;
import com.ghgande.j2mod.modbus.msg.WriteMultipleRegistersRequest;
import com.ghgande.j2mod.modbus.msg.WriteSingleRegisterRequest;
import com.ghgande.j2mod.modbus.procimg.InputRegister;
import com.ghgande.j2mod.modbus.procimg.Register;
import com.ghgande.j2mod.modbus.util.BitVector;

import io.openems.api.bridge.Bridge;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.OpenemsModbusException;
import io.openems.impl.protocol.modbus.internal.range.ModbusInputRegisterRange;
import io.openems.impl.protocol.modbus.internal.range.ModbusRange;

@ThingInfo(title = "Modbus")
public abstract class ModbusBridge extends Bridge {

	/*
	 * Abstract Methods
	 */
	public abstract ModbusTransaction getTransaction() throws OpenemsModbusException;

	protected abstract void closeModbusConnection();

	/*
	 * Static Methods
	 */
	static boolean[] toBooleanArray(byte[] bytes) {
		BitSet bits = BitSet.valueOf(bytes);
		boolean[] bools = new boolean[bytes.length * 8];
		for (int i = bits.nextSetBit(0); i != -1; i = bits.nextSetBit(i + 1)) {
			bools[i] = true;
		}
		return bools;
	}

	/*
	 * Methods
	 */

	@Override
	protected boolean initialize() {
		/*
		 * Create a new SerialConnection
		 */
		closeModbusConnection();
		return true;
	}

	protected InputRegister[] query(int modbusUnitId, ModbusRange range) throws OpenemsModbusException {
		if (range instanceof ModbusInputRegisterRange) {
			return queryInputRegisters(modbusUnitId, range.getStartAddress(), range.getLength());
		} else {
			return queryMultipleRegisters(modbusUnitId, range.getStartAddress(), range.getLength());
		}
	}

	protected boolean[] queryCoil(int modbusUnitId, ModbusRange range) throws OpenemsModbusException {
		return queryCoils(modbusUnitId, range.getStartAddress(), range.getLength());
	}

	private boolean[] queryCoils(int modbusUnitId, int startAddress, int length) throws OpenemsModbusException {
		ModbusTransaction trans = getTransaction();
		ReadCoilsRequest req = new ReadCoilsRequest(startAddress, length);
		req.setUnitID(modbusUnitId);
		trans.setRequest(req);
		try {
			trans.execute();
		} catch (ModbusException e) {
			// try again with new connection
			closeModbusConnection();
			trans = getTransaction();
			req = new ReadCoilsRequest(startAddress, length);
			req.setUnitID(modbusUnitId);
			trans.setRequest(req);
			try {
				trans.execute();
			} catch (ModbusException e1) {
				throw new OpenemsModbusException("Error on modbus query. " //
						+ "UnitId [" + modbusUnitId + "], Address [" + startAddress + "/0x"
						+ Integer.toHexString(startAddress) + "], Count [" + length + "]: " + e1.getMessage());
			}
		}
		ModbusResponse res = trans.getResponse();
		if (res instanceof ReadCoilsResponse) {
			ReadCoilsResponse mres = (ReadCoilsResponse) res;
			return toBooleanArray(mres.getCoils().getBytes());
		} else {
			throw new OpenemsModbusException("Unable to read modbus response. " //
					+ "UnitId [" + modbusUnitId + "], Address [" + startAddress + "], Count [" + length + "]: "
					+ res.toString());
		}
	}

	protected void write(int modbusUnitId, int address, List<Register> registers) throws OpenemsModbusException {
		write(modbusUnitId, address, registers.toArray(new Register[registers.size()]));
	}

	protected void write(int modbusUnitId, int address, Register... register) throws OpenemsModbusException {
		ModbusResponse res;
		try {
			if (register.length == 0) {
				return;
			} else if (register.length == 1) {
				res = writeSingleRegister(modbusUnitId, address, register[0]);
			} else {
				res = writeMultipleRegisters(modbusUnitId, address, register);
			}
		} catch (ModbusException | OpenemsModbusException e) {
			throw new OpenemsModbusException("Error on modbus write. " //
					+ "UnitId [" + modbusUnitId + "], Address [" + address + "/0x" + Integer.toHexString(address)
					+ "], Register [" + registersAsString(register) + "]: " + e.getMessage());
		}
		if (res instanceof ExceptionResponse) {
			throw new OpenemsModbusException("Error on modbus write response. " //
					+ "UnitId [" + modbusUnitId + "], Address [" + address + "/0x" + Integer.toHexString(address)
					+ "], Register [" + registersAsString(register) + "]: " + res.toString());
		}
		log.debug("Successful write. " //
				+ "UnitId [" + modbusUnitId + "], Address [" + address + "/0x" + Integer.toHexString(address)
				+ "], Register [" + registersAsString(register) + "]");
	}

	protected void writeCoil(int modbusUnitId, int address, List<Boolean> coils) throws OpenemsModbusException {
		writeCoil(modbusUnitId, address, coils.toArray(new Boolean[coils.size()]));
	}

	protected void writeCoil(int modbusUnitId, int address, Boolean... values) throws OpenemsModbusException {
		boolean[] coils = new boolean[values.length];
		for (int i = 0; i < values.length; i++) {
			coils[i] = values[i];
		}
		ModbusResponse res;
		try {
			if (coils.length == 0) {
				return;
			} else if (coils.length == 1) {
				res = writeSingleCoil(modbusUnitId, address, coils[0]);
			} else {
				res = writeMultipleCoils(modbusUnitId, address, coils);
			}
		} catch (ModbusException | OpenemsModbusException e) {
			throw new OpenemsModbusException("Error on modbus write. " //
					+ "UnitId [" + modbusUnitId + "], Address [" + address + "/0x" + Integer.toHexString(address)
					+ "], Coil [" + coilsAsString(coils) + "]: " + e.getMessage());
		}
		if (res instanceof ExceptionResponse) {
			throw new OpenemsModbusException("Error on modbus write response. " //
					+ "UnitId [" + modbusUnitId + "], Address [" + address + "/0x" + Integer.toHexString(address)
					+ "], Coil [" + coilsAsString(coils) + "]: " + res.toString());
		}
		log.debug("Successful write. " //
				+ "UnitId [" + modbusUnitId + "], Address [" + address + "/0x" + Integer.toHexString(address)
				+ "], Coil [" + coilsAsString(coils) + "]");
	}

	private String coilsAsString(boolean[] coils) {
		StringJoiner joiner = new StringJoiner(",");
		for (boolean coil : coils) {
			joiner.add(String.valueOf(coil));
		}
		return joiner.toString();
	}

	private ModbusResponse writeMultipleCoils(int modbusUnitId, int address, boolean[] coils)
			throws OpenemsModbusException, ModbusException {
		ModbusTransaction trans = getTransaction();
		BitVector vec = new BitVector(coils.length);
		for (int i = 0; i < coils.length; i++) {
			vec.setBit(i, coils[i]);
		}
		WriteMultipleCoilsRequest req = new WriteMultipleCoilsRequest(address, vec);
		req.setUnitID(modbusUnitId);
		trans.setRequest(req);
		try {
			trans.execute();
		} catch (ModbusException e) {
			// try again with new connection
			closeModbusConnection();
			trans = getTransaction();
			req = new WriteMultipleCoilsRequest(address, vec);
			req.setUnitID(modbusUnitId);
			trans.setRequest(req);
			trans.execute();
		}
		return trans.getResponse();
	}

	private ModbusResponse writeSingleCoil(int modbusUnitId, int address, boolean b)
			throws OpenemsModbusException, ModbusException {
		ModbusTransaction trans = getTransaction();
		WriteCoilRequest req = new WriteCoilRequest(address, b);
		req.setUnitID(modbusUnitId);
		trans.setRequest(req);
		try {
			trans.execute();
		} catch (ModbusException e) {
			// try again with new connection
			closeModbusConnection();
			trans = getTransaction();
			req = new WriteCoilRequest(address, b);
			req.setUnitID(modbusUnitId);
			trans.setRequest(req);
			trans.execute();
		}
		return trans.getResponse();
	}

	private ModbusResponse writeSingleRegister(int modbusUnitId, int address, Register register)
			throws ModbusException, OpenemsModbusException {
		ModbusTransaction trans = getTransaction();
		WriteSingleRegisterRequest req = new WriteSingleRegisterRequest(address, register);
		req.setUnitID(modbusUnitId);
		trans.setRequest(req);
		try {
			trans.execute();
		} catch (ModbusException e) {
			// try again with new connection
			closeModbusConnection();
			trans = getTransaction();
			req = new WriteSingleRegisterRequest(address, register);
			req.setUnitID(modbusUnitId);
			trans.setRequest(req);
			trans.execute();
		}
		return trans.getResponse();
	}

	/**
	 * Write Multiple Registers (function code 16)
	 *
	 * @param modbusUnitId
	 * @param address
	 * @param register
	 * @throws OpenemsModbusException
	 * @throws ModbusException
	 */
	private ModbusResponse writeMultipleRegisters(int modbusUnitId, int address, Register... register)
			throws OpenemsModbusException, ModbusException {
		ModbusTransaction trans = getTransaction();
		WriteMultipleRegistersRequest req = new WriteMultipleRegistersRequest(address, register);
		req.setUnitID(modbusUnitId);
		trans.setRequest(req);
		try {
			trans.execute();
		} catch (ModbusException e) {
			// try again with new connection
			closeModbusConnection();
			trans = getTransaction();
			req = new WriteMultipleRegistersRequest(address, register);
			req.setUnitID(modbusUnitId);
			trans.setRequest(req);
			trans.execute();
		}
		return trans.getResponse();
	}

	private String registersAsString(Register... registers) {
		StringJoiner joiner = new StringJoiner(",");
		for (Register register : registers) {
			joiner.add(String.valueOf(register.getValue()));
		}
		return joiner.toString();
	}

	/**
	 * Executes a query on the Modbus client
	 *
	 * @param modbusUnitId
	 * @param address
	 * @param count
	 * @return
	 * @throws OpenemsModbusException
	 */
	private Register[] queryMultipleRegisters(int modbusUnitId, int address, int count) throws OpenemsModbusException {
		ModbusTransaction trans = getTransaction();
		ReadMultipleRegistersRequest req = new ReadMultipleRegistersRequest(address, count);
		req.setUnitID(modbusUnitId);
		trans.setRequest(req);
		try {
			trans.execute();
		} catch (ModbusException e) {
			// try again with new connection
			closeModbusConnection();
			trans = getTransaction();
			req = new ReadMultipleRegistersRequest(address, count);
			req.setUnitID(modbusUnitId);
			trans.setRequest(req);
			try {
				trans.execute();
			} catch (ModbusException e1) {
				throw new OpenemsModbusException("Error on modbus query. " //
						+ "UnitId [" + modbusUnitId + "], Address [" + address + "/0x" + Integer.toHexString(address)
						+ "], Count [" + count + "]: " + e1.getMessage());
			}
		}
		ModbusResponse res = trans.getResponse();
		if (res instanceof ReadMultipleRegistersResponse) {
			ReadMultipleRegistersResponse mres = (ReadMultipleRegistersResponse) res;
			return mres.getRegisters();
		} else {
			throw new OpenemsModbusException("Unable to read modbus response. " //
					+ "UnitId [" + modbusUnitId + "], Address [" + address + "], Count [" + count + "]: "
					+ res.toString());
		}
	}

	/**
	 * Executes a query on the Modbus client
	 *
	 * @param modbusUnitId
	 * @param address
	 * @param count
	 * @return
	 * @throws OpenemsModbusException
	 */
	private InputRegister[] queryInputRegisters(int modbusUnitId, int address, int count)
			throws OpenemsModbusException {
		ModbusTransaction trans = getTransaction();
		ReadInputRegistersRequest req = new ReadInputRegistersRequest(address, count);
		req.setUnitID(modbusUnitId);
		trans.setRequest(req);
		try {
			trans.execute();
		} catch (ModbusException e) {
			// try again with new connection
			closeModbusConnection();
			trans = getTransaction();
			req = new ReadInputRegistersRequest(address, count);
			req.setUnitID(modbusUnitId);
			trans.setRequest(req);
			try {
				trans.execute();
			} catch (ModbusException e1) {
				throw new OpenemsModbusException("Error on modbus query. " //
						+ "UnitId [" + modbusUnitId + "], Address [" + address + "/0x" + Integer.toHexString(address)
						+ "], Count [" + count + "]: " + e1.getMessage());
			}
		}
		ModbusResponse res = trans.getResponse();
		if (res instanceof ReadInputRegistersResponse) {
			ReadInputRegistersResponse mres = (ReadInputRegistersResponse) res;
			return mres.getRegisters();
		} else {
			throw new OpenemsModbusException("Unable to read modbus response. " //
					+ "UnitId [" + modbusUnitId + "], Address [" + address + "], Count [" + count + "]: "
					+ res.toString());
		}
	}
}
