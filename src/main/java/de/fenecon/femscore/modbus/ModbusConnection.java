package de.fenecon.femscore.modbus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.wimpi.modbus.ModbusException;
import net.wimpi.modbus.io.ModbusTransaction;
import net.wimpi.modbus.msg.ModbusResponse;
import net.wimpi.modbus.msg.ReadMultipleRegistersRequest;
import net.wimpi.modbus.msg.ReadMultipleRegistersResponse;
import net.wimpi.modbus.msg.WriteSingleRegisterRequest;
import net.wimpi.modbus.msg.WriteSingleRegisterResponse;
import net.wimpi.modbus.procimg.Register;

public abstract class ModbusConnection implements AutoCloseable {
	@SuppressWarnings("unused")
	private final static Logger log = LoggerFactory.getLogger(ModbusConnection.class);

	protected final int cycle; // length of a query cycle in milliseconds

	public ModbusConnection(int cycle) {
		this.cycle = cycle;
	}

	protected abstract ModbusTransaction getTransaction() throws Exception;

	public int getCycle() {
		return cycle;
	}

	public synchronized Register[] query(int unitid, int ref, int count) throws Exception {
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

	public synchronized void write(int unitid, int ref, Register reg) throws Exception {
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

	@Override
	public abstract void close();
}
