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
import java.util.Arrays;
import java.util.List;

import com.ghgande.j2mod.modbus.Modbus;
import com.ghgande.j2mod.modbus.io.ModbusSerialTransaction;
import com.ghgande.j2mod.modbus.io.ModbusTransaction;
import com.ghgande.j2mod.modbus.net.SerialConnection;
import com.ghgande.j2mod.modbus.util.SerialParameters;

import io.openems.api.device.Device;
import io.openems.api.exception.OpenemsModbusException;
import io.openems.api.thing.IsConfig;

public class ModbusRtu extends ModbusBridge {
	private volatile Integer baudrate = null;
	private SerialConnection connection = null;
	private volatile Integer databits = null;
	private volatile String parity = null;
	private volatile String serialinterface = null;
	private volatile Integer stopbits = null;

	@Override
	public void dispose() {

	}

	@Override
	public ModbusTransaction getTransaction() throws OpenemsModbusException {
		establishModbusConnection(connection);
		ModbusSerialTransaction trans = new ModbusSerialTransaction(connection);
		trans.setRetries(0);
		return trans;
	}

	@IsConfig("baudrate")
	public void setBaudrate(Integer baudrate) {
		this.baudrate = baudrate;
		triggerInitialize();
	}

	@IsConfig("databits")
	public void setDatabits(Integer databits) {
		this.databits = databits;
	}

	@Override
	public void setDevices(Device... devices) {
		super.setDevices(devices);
		triggerInitialize();
	}

	@IsConfig("parity")
	public void setParity(String parity) {
		this.parity = parity;
	}

	@IsConfig("serialinterface")
	public void setSerialinterface(String serialinterface) {
		this.serialinterface = serialinterface;
		triggerInitialize();
	}

	@IsConfig("stopbits")
	public void setStopbits(Integer stopbits) {
		this.stopbits = stopbits;
	}

	@Override
	public String toString() {
		return "ModbusRtu [baudrate=" + baudrate + ", serialinterface=" + serialinterface + ", devices="
				+ Arrays.toString(devices) + "]";
	}

	@Override
	protected boolean initialize() {
		if (baudrate == null || databits == null || parity == null || serialinterface == null || stopbits == null
				|| devices == null || devices.length == 0) {
			return false;
		}
		/*
		 * Copy and cast devices to local modbusdevices array
		 */
		List<ModbusDevice> modbusdevices = new ArrayList<>();
		for (Device device : devices) {
			if (device instanceof ModbusDevice) {
				modbusdevices.add((ModbusDevice) device);
			}
		}
		this.modbusdevices = modbusdevices.stream().toArray(ModbusDevice[]::new);
		/*
		 * Create a new SerialConnection
		 */
		if (connection != null && connection.isOpen()) {
			connection.close();
		}
		SerialParameters params = new SerialParameters();
		params.setPortName(serialinterface);
		params.setBaudRate(baudrate);
		params.setDatabits(databits);
		params.setParity(parity);
		params.setStopbits(stopbits);
		params.setEncoding(Modbus.SERIAL_ENCODING_RTU);
		params.setEcho(false);
		connection = new SerialConnection(params);
		try {
			connection.open();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	private void establishModbusConnection(SerialConnection connection) throws OpenemsModbusException {
		if (!connection.isOpen()) {
			try {
				connection.open();
			} catch (Exception e) {
				throw new OpenemsModbusException("Unable to open modbus connection: " + connection);
			}
		}
	}
}
