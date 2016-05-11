package de.fenecon.femscore.modbus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.io.ModbusTransaction;
import com.ghgande.j2mod.modbus.msg.ModbusResponse;
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersRequest;
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersResponse;
import com.ghgande.j2mod.modbus.msg.WriteMultipleRegistersRequest;
import com.ghgande.j2mod.modbus.msg.WriteMultipleRegistersResponse;
import com.ghgande.j2mod.modbus.msg.WriteSingleRegisterRequest;
import com.ghgande.j2mod.modbus.msg.WriteSingleRegisterResponse;
import com.ghgande.j2mod.modbus.procimg.Register;

import de.fenecon.femscore.modbus.protocol.Element;
import de.fenecon.femscore.modbus.protocol.ElementRange;
import de.fenecon.femscore.modbus.protocol.ModbusProtocol;
import de.fenecon.femscore.modbus.protocol.interfaces.DoublewordElement;
import de.fenecon.femscore.modbus.protocol.interfaces.WordElement;

public abstract class ModbusConnection implements AutoCloseable {
	private final static Logger log = LoggerFactory.getLogger(ModbusConnection.class);

	protected final int cycle; // length of a query cycle in milliseconds

	public ModbusConnection(int cycle) {
		this.cycle = cycle;
	}

	protected abstract ModbusTransaction getTransaction() throws Exception;

	public abstract void dispose();

	public int getCycle() {
		return cycle;
	}

	private synchronized Register[] singleQuery(int unitid, int ref, int count) throws Exception {
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

	public Register[] query(int unitid, int ref, int count) throws Exception {
		try {
			return singleQuery(unitid, ref, count);
		} catch (Exception e) {
			log.info("Query-Exception: {}. Try again with new connection", e.getMessage());
			this.close();
			return singleQuery(unitid, ref, count);
		}
	}

	public void updateProtocol(int unitid, ModbusProtocol protocol) throws Exception {
		for (ElementRange elementRange : protocol.getElementRanges()) {
			Register[] registers = query(unitid, elementRange.getStartAddress(), elementRange.getTotalLength());
			int position = 0;
			for (Element<?> element : elementRange.getElements()) {
				int length = element.getLength();
				if (element instanceof WordElement) {
					((WordElement) element).update(registers[position]);
				} else if (element instanceof DoublewordElement) {
					((DoublewordElement) element).update(registers[position], registers[position + 1]);
				}
				position += length;
			}
		}
	}

	private synchronized void singleWrite(int unitid, int ref, Register reg) throws Exception {
		ModbusTransaction trans = getTransaction();
		WriteSingleRegisterRequest req = new WriteSingleRegisterRequest(ref, reg);
		req.setUnitID(unitid);
		trans.setRequest(req);
		trans.execute();
		ModbusResponse res = trans.getResponse();
		if (!(res instanceof WriteSingleRegisterResponse)) {
			throw new ModbusException(res.toString());
		}
	}

	public void write(int unitid, int ref, Register reg) throws Exception {
		try {
			singleWrite(unitid, ref, reg);
		} catch (Exception e) {
			log.info("Write-Exception: {}. Try again with new connection", e.getMessage());
			this.close();
			singleWrite(unitid, ref, reg);
		}
	}

	private synchronized void singleWrite(int unitid, int ref, Register[] regs) throws Exception {
		ModbusTransaction trans = getTransaction();
		WriteMultipleRegistersRequest req = new WriteMultipleRegistersRequest(ref, regs);
		req.setUnitID(unitid);
		trans.setRequest(req);
		trans.execute();
		ModbusResponse res = trans.getResponse();
		if (!(res instanceof WriteMultipleRegistersResponse)) {
			throw new ModbusException(res.toString());
		}
	}

	public void write(int unitid, int ref, Register[] regs) throws Exception {
		try {
			singleWrite(unitid, ref, regs);
		} catch (Exception e) {
			log.info("Write-Exception: {}. Try again with new connection", e.getMessage());
			this.close();
			singleWrite(unitid, ref, regs);
		}
	}

	@Override
	public abstract void close();
}
