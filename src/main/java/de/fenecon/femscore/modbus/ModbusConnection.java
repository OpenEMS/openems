package de.fenecon.femscore.modbus;

import net.wimpi.modbus.ModbusException;
import net.wimpi.modbus.io.ModbusTransaction;
import net.wimpi.modbus.msg.ModbusResponse;
import net.wimpi.modbus.msg.ReadMultipleRegistersRequest;
import net.wimpi.modbus.msg.ReadMultipleRegistersResponse;
import net.wimpi.modbus.msg.WriteSingleRegisterRequest;
import net.wimpi.modbus.msg.WriteSingleRegisterResponse;
import net.wimpi.modbus.procimg.Register;

public abstract class ModbusConnection implements AutoCloseable {
	protected abstract ModbusTransaction getTransaction() throws Exception;

	public Register[] getModbusResponse(int unitid, int ref, int count) throws Exception {
		ModbusTransaction trans = getTransaction();
		ReadMultipleRegistersRequest req = new ReadMultipleRegistersRequest(ref, count);
		req.setUnitID(unitid);
		trans.setRequest(req);
		trans.execute();
		ModbusResponse res = trans.getResponse();
		if (res instanceof ReadMultipleRegistersResponse) {
			ReadMultipleRegistersResponse mres = (ReadMultipleRegistersResponse) res;
			return mres.getRegisters();
		} else {
			throw new ModbusException(res.toString());
		}
	}

	public void writeRegister(int unitid, int ref, Register reg) throws Exception {
		ModbusTransaction trans = getTransaction();
		WriteSingleRegisterRequest req = new WriteSingleRegisterRequest(ref, reg);
		req.setUnitID(unitid);
		trans.setRequest(req);
		trans.execute();
		ModbusResponse res = trans.getResponse();
		if (res instanceof WriteSingleRegisterResponse) {
			WriteSingleRegisterResponse wres = (WriteSingleRegisterResponse) res;
			System.out.println(wres.toString());
		} else {
			throw new ModbusException(res.toString());
		}
	}

	/*public int getUInt(int ref) throws Exception {
		Register[] registers = getModbusResponse(ref, 1);
		return registers[0].getValue();
	}*/

	@Override
	public abstract void close();
}
