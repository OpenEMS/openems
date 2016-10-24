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

import java.net.Inet4Address;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ghgande.j2mod.modbus.io.ModbusTCPTransaction;
import com.ghgande.j2mod.modbus.io.ModbusTransaction;
import com.ghgande.j2mod.modbus.net.TCPMasterConnection;

import io.openems.api.device.Device;
import io.openems.api.exception.OpenemsModbusException;
import io.openems.api.thing.IsConfig;

public class ModbusTcp extends ModbusBridge {
	private static Logger log = LoggerFactory.getLogger(ModbusTcp.class);
	private Optional<TCPMasterConnection> connection = Optional.empty();
	private volatile Optional<Inet4Address> ip = Optional.empty();
	private volatile int port = 502;

	@Override
	public void dispose() {

	}

	@Override
	public ModbusTransaction getTransaction() throws OpenemsModbusException {
		TCPMasterConnection connection = getModbusConnection();
		ModbusTCPTransaction trans = new ModbusTCPTransaction(connection);
		return trans;
	}

	@IsConfig("ip")
	public void setBaudrate(Inet4Address ip) {
		this.ip = Optional.ofNullable(ip);
		triggerInitialize();
	}

	@Override
	public void setDevices(Device... devices) {
		super.setDevices(devices);
		triggerInitialize();
	}

	@Override
	public String toString() {
		return "ModbusTcp [ip=" + ip + "]";
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
		if (connection.isPresent() && connection.get().isConnected()) {
			connection.get().close();
		}
		connection = Optional.empty();
	}

	private TCPMasterConnection getModbusConnection() throws OpenemsModbusException {
		if (!connection.isPresent()) {
			if (!ip.isPresent()) {
				throw new OpenemsModbusException("Modbus-TCP is not configured completely");
			}
			connection = Optional.of(new TCPMasterConnection(ip.get()));
			connection.get().setPort(port);
		}
		if (!connection.get().isConnected()) {
			try {
				connection.get().connect();
			} catch (Exception e) {
				throw new OpenemsModbusException("Unable to open Modbus-TCP connection: " + connection);
			}
		}
		return connection.get();
	}
}
