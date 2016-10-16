package io.openems.impl.protocol.modbus.bridge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ghgande.j2mod.modbus.io.ModbusTransaction;
import com.ghgande.j2mod.modbus.msg.ModbusResponse;
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersRequest;
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersResponse;
import com.ghgande.j2mod.modbus.procimg.Register;

import io.openems.api.exception.OpenemsModbusException;
import io.openems.core.bridge.Bridge;
import io.openems.impl.protocol.modbus.device.ModbusDevice;
import io.openems.impl.protocol.modbus.internal.ModbusRange;

public abstract class ModbusBridge extends Bridge {
	private final static Logger log = LoggerFactory.getLogger(ModbusBridge.class);

	protected volatile ModbusDevice[] modbusdevices = new ModbusDevice[0];

	@Override
	public abstract void dispose();

	public abstract ModbusTransaction getTransaction() throws OpenemsModbusException;

	public Register[] query(int modbusUnitId, ModbusRange range) throws OpenemsModbusException {
		return singleQuery(modbusUnitId, range.getStartAddress(), range.getLength());
	}

	@Override
	protected void forever() throws Throwable {
		log.info("forever()");
		for (ModbusDevice modbusdevice : modbusdevices) {
			modbusdevice.update(this);
		}
		// TODO add cycle
		Thread.sleep(1000);
	}

	private Register[] singleQuery(int modbusUnitId, int address, int count) throws OpenemsModbusException {
		ModbusTransaction trans = getTransaction();
		ReadMultipleRegistersRequest req = new ReadMultipleRegistersRequest(address, count);
		req.setUnitID(modbusUnitId);
		trans.setRequest(req);
		try {
			trans.execute();
		} catch (com.ghgande.j2mod.modbus.ModbusException e) {
			throw new OpenemsModbusException("Error while executing modbus transaction. " //
					+ "UnitId [" + modbusUnitId + "], Address [" + address + "], Count [" + count + "]: "
					+ e.getMessage());
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
}
