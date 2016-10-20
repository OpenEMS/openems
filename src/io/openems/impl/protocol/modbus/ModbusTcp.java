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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
	private TCPMasterConnection connection = null;
	private volatile Inet4Address ip = null;
	private volatile int port = 502;

	@Override
	public void dispose() {

	}

	@Override
	public ModbusTransaction getTransaction() throws OpenemsModbusException {
		establishModbusConnection(connection);
		ModbusTCPTransaction trans = new ModbusTCPTransaction(connection);
		return trans;
	}

	@IsConfig("ip")
	public void setBaudrate(Inet4Address ip) {
		this.ip = ip;
		triggerInitialize();
	}

	@Override
	public void setDevices(Device... devices) {
		super.setDevices(devices);
		triggerInitialize();
	}

	@Override
	public String toString() {
		return "ModbusTcp [ip=" + ip + ", devices=" + Arrays.toString(devices) + "]";
	}

	@Override
	protected boolean initialize() {
		if (ip == null || devices == null || devices.length == 0) {
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
		 * Create a new TCPMasterConnection
		 */
		if (connection != null && connection.isConnected()) {
			connection.close();
		}
		connection = new TCPMasterConnection(ip);
		connection.setPort(port);
		try {
			establishModbusConnection(connection);
		} catch (OpenemsModbusException e) {
			log.error("Unable to open Modbus-TCP connection: " + e.getMessage());
			return false;
		}
		return true;
	}

	private void establishModbusConnection(TCPMasterConnection connection) throws OpenemsModbusException {
		if (!connection.isConnected()) {
			try {
				connection.connect();
			} catch (Exception e) {
				throw new OpenemsModbusException("Unable to open modbus connection: " + e.getMessage());
			}
		}
	}
}
