package de.fenecon.femscore.modbus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fenecon.femscore.modbus.protocol.Element;
import de.fenecon.femscore.modbus.protocol.ElementRange;
import de.fenecon.femscore.modbus.protocol.ModbusProtocol;
import de.fenecon.femscore.modbus.protocol.interfaces.DoublewordElement;
import de.fenecon.femscore.modbus.protocol.interfaces.WordElement;
import net.wimpi.modbus.ModbusException;
import net.wimpi.modbus.io.ModbusTransaction;
import net.wimpi.modbus.msg.ModbusResponse;
import net.wimpi.modbus.msg.ReadMultipleRegistersRequest;
import net.wimpi.modbus.msg.ReadMultipleRegistersResponse;
import net.wimpi.modbus.msg.WriteMultipleRegistersRequest;
import net.wimpi.modbus.msg.WriteMultipleRegistersResponse;
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

	public abstract void dispose();

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

	public synchronized void write(int unitid, int ref, Register reg) throws Exception {
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

	public synchronized void write(int unitid, int ref, Register[] regs) throws Exception {
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

	@Override
	public abstract void close();
}
