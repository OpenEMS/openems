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

public abstract class ModbusBridge extends Bridge {
	private final static Logger log = LoggerFactory.getLogger(ModbusBridge.class);

	protected volatile ModbusDevice[] modbusdevices = new ModbusDevice[0];

	@Override
	public abstract void dispose();

	@Override
	protected void forever() throws Throwable {
		log.info("forever()");
		for (ModbusDevice modbusdevice : modbusdevices) {
			modbusdevice.update(getTransaction());
		}
		Thread.sleep(1000);
	}

	protected abstract ModbusTransaction getTransaction() throws OpenemsModbusException;

	protected Register[] singleQuery(int unitid, int address, int count) throws OpenemsModbusException {
		ModbusTransaction trans = getTransaction();
		ReadMultipleRegistersRequest req = new ReadMultipleRegistersRequest(address, count);
		req.setUnitID(unitid);
		trans.setRequest(req);
		try {
			trans.execute();
		} catch (com.ghgande.j2mod.modbus.ModbusException e) {
			throw new OpenemsModbusException("Error while executing modbus transaction. " //
					+ "UnitId [" + unitid + "], Address [" + address + "], Count [" + count + "]: " + e.getMessage());
		}
		ModbusResponse res = trans.getResponse();
		if (res instanceof ReadMultipleRegistersResponse) {
			ReadMultipleRegistersResponse mres = (ReadMultipleRegistersResponse) res;
			return mres.getRegisters();
		} else {
			throw new OpenemsModbusException("Unable to read modbus response. " //
					+ "UnitId [" + unitid + "], Address [" + address + "], Count [" + count + "]: " + res.toString());
		}
	}
}
