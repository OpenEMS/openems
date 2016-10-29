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

import java.util.Optional;

import com.ghgande.j2mod.modbus.Modbus;
import com.ghgande.j2mod.modbus.io.ModbusSerialTransaction;
import com.ghgande.j2mod.modbus.io.ModbusTransaction;
import com.ghgande.j2mod.modbus.net.SerialConnection;
import com.ghgande.j2mod.modbus.util.SerialParameters;

import io.openems.api.device.Device;
import io.openems.api.exception.OpenemsModbusException;
import io.openems.api.thing.IsConfig;

public class ModbusRtu extends ModbusBridge {
	private volatile Optional<Integer> baudrate = Optional.empty();
	private Optional<SerialConnection> connection = Optional.empty();
	private volatile Optional<Integer> databits = Optional.empty();
	private volatile Optional<String> parity = Optional.empty();
	private volatile Optional<String> serialinterface = Optional.empty();
	private volatile Optional<Integer> stopbits = Optional.empty();

	@Override
	public void dispose() {

	}

	@Override
	public ModbusTransaction getTransaction() throws OpenemsModbusException {
		SerialConnection connection = getModbusConnection();
		ModbusSerialTransaction trans = new ModbusSerialTransaction(connection);
		trans.setRetries(0);
		return trans;
	}

	@IsConfig("baudrate")
	public void setBaudrate(Integer baudrate) {
		this.baudrate = Optional.ofNullable(baudrate);
		triggerInitialize();
	}

	@IsConfig("databits")
	public void setDatabits(Integer databits) {
		this.databits = Optional.ofNullable(databits);
	}

	@Override
	public void addDevice(Device device) {
		super.addDevice(device);
		triggerInitialize();
	}

	@IsConfig("parity")
	public void setParity(String parity) {
		this.parity = Optional.ofNullable(parity);
	}

	@IsConfig("serialinterface")
	public void setSerialinterface(String serialinterface) {
		this.serialinterface = Optional.ofNullable(serialinterface);
		triggerInitialize();
	}

	@IsConfig("stopbits")
	public void setStopbits(Integer stopbits) {
		this.stopbits = Optional.ofNullable(stopbits);
	}

	@Override
	public String toString() {
		return "ModbusRtu [baudrate=" + baudrate + ", serialinterface=" + serialinterface + "]";
	}

	@Override
	protected boolean initialize() {
		if (!super.initialize()) {
			return false;
		}
		try {
			getModbusConnection();
		} catch (OpenemsModbusException e) {
			log.error(e.getMessage());
			return false;
		}
		return true;
	}

	@Override
	protected void closeModbusConnection() {
		if (connection.isPresent() && connection.get().isOpen()) {
			connection.get().close();
		}
		connection = Optional.empty();
	}

	private SerialConnection getModbusConnection() throws OpenemsModbusException {
		if (!connection.isPresent()) {
			if (!baudrate.isPresent() || !databits.isPresent() || !parity.isPresent() || !serialinterface.isPresent()
					|| !stopbits.isPresent()) {
				throw new OpenemsModbusException("Modbus-RTU is not configured completely");
			}
			SerialParameters params = new SerialParameters();
			params.setPortName(serialinterface.get());
			params.setBaudRate(baudrate.get());
			params.setDatabits(databits.get());
			params.setParity(parity.get());
			params.setStopbits(stopbits.get());
			params.setEncoding(Modbus.SERIAL_ENCODING_RTU);
			params.setEcho(false);
			connection = Optional.of(new SerialConnection(params));
		}
		if (!connection.get().isOpen()) {
			try {
				connection.get().open();
			} catch (Exception e) {
				throw new OpenemsModbusException("Unable to open Modbus-RTU connection: " + connection);
			}
		}
		return connection.get();
	}
}
