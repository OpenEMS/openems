/*******************************************************************************
 * OpenEMS - Open Source Energy Management System
 * Copyright (c) 2016 FENECON GmbH and contributors
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.io.ModbusTransaction;
import com.ghgande.j2mod.modbus.msg.ModbusResponse;
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersRequest;
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersResponse;
import com.ghgande.j2mod.modbus.msg.WriteSingleRegisterRequest;
import com.ghgande.j2mod.modbus.msg.WriteSingleRegisterResponse;
import com.ghgande.j2mod.modbus.procimg.Register;

import io.openems.api.bridge.Bridge;
import io.openems.api.device.Device;
import io.openems.api.exception.OpenemsException;
import io.openems.api.exception.OpenemsModbusException;
import io.openems.impl.protocol.modbus.internal.ModbusRange;

public abstract class ModbusBridge extends Bridge {
	protected volatile ModbusDevice[] modbusdevices = new ModbusDevice[0];
	private AtomicBoolean isWriteTriggered = new AtomicBoolean(false);

	@Override
	public abstract void dispose();

	public abstract ModbusTransaction getTransaction() throws OpenemsModbusException;

	@Override
	public void triggerWrite() {
		// set the Write-flag
		isWriteTriggered.set(true);
		// start "run()" again as fast as possible
		triggerForceRun();
	}

	@Override
	protected void forever() {
		for (ModbusDevice modbusdevice : modbusdevices) {
			// if Write-flag was set -> start writing for all Devices immediately
			if (isWriteTriggered.get()) {
				isWriteTriggered.set(false);
				writeAllDevices();
			}
			// Update this Device
			try {
				modbusdevice.update(this);
			} catch (OpenemsException e) {
				log.error(e.getMessage());
			}
		}
	}

	protected abstract void closeModbusConnection();

	@Override
	protected boolean initialize() {
		/*
		 * Copy and cast devices to local modbusdevices array
		 */
		if (!devices.isPresent() || devices.get().length == 0) {
			return false;
		}
		List<ModbusDevice> modbusdevices = new ArrayList<>();
		for (Device device : devices.get()) {
			if (device instanceof ModbusDevice) {
				modbusdevices.add((ModbusDevice) device);
			}
		}
		ModbusDevice[] newModbusdevices = modbusdevices.stream().toArray(ModbusDevice[]::new);
		if (newModbusdevices == null) {
			newModbusdevices = new ModbusDevice[0];
		}
		this.modbusdevices = newModbusdevices;
		/*
		 * Create a new SerialConnection
		 */
		closeModbusConnection();
		return true;
	}

	protected Register[] query(int modbusUnitId, ModbusRange range) throws OpenemsModbusException {
		return query(modbusUnitId, range.getStartAddress(), range.getLength());
	}

	protected void write(int modbusUnitId, int address, Register register) throws OpenemsModbusException {
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
			try {
				trans.execute();
			} catch (ModbusException e1) {
				throw new OpenemsModbusException("Error while executing write transaction. " //
						+ "UnitId [" + modbusUnitId + "], Address [" + address + "], Register [" + register.getValue()
						+ "]: " + e1.getMessage());
			}
		}
		ModbusResponse res = trans.getResponse();
		if (!(res instanceof WriteSingleRegisterResponse)) {
			throw new OpenemsModbusException("Unable to read modbus write response. " //
					+ "UnitId [" + modbusUnitId + "], Address [" + address + "], Register [" + register.getValue()
					+ "]: " + res.toString());
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
	private Register[] query(int modbusUnitId, int address, int count) throws OpenemsModbusException {
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
						+ "UnitId [" + modbusUnitId + "], Address [" + address + "], Count [" + count + "]: "
						+ e1.getMessage());
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

	private void writeAllDevices() {
		for (ModbusDevice modbusdevice : modbusdevices) {
			try {
				modbusdevice.write(this);
			} catch (OpenemsException e) {
				log.error("Error while writing to ModbusDevice [" + modbusdevice.getThingId() + "]: " + e.getMessage());
			}
		}
	}
}
