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

import io.openems.api.channel.Channel;
import io.openems.api.channel.ChannelUpdateListener;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.device.Device;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.OpenemsModbusException;

public class ModbusTcp extends ModbusBridge implements ChannelUpdateListener {
	private static Logger log = LoggerFactory.getLogger(ModbusTcp.class);
	private Optional<TCPMasterConnection> connection = Optional.empty();

	private final static int MODBUS_PORT = 502;

	/*
	 * Config
	 */
	public final ConfigChannel<Inet4Address> ip = new ConfigChannel<Inet4Address>("ip", this, Inet4Address.class)
			.updateListener(this);
	public final ConfigChannel<Integer> port = new ConfigChannel<Integer>("port", this, Integer.class)
			.defaultValue(MODBUS_PORT).updateListener(this);

	@Override protected int getCycleTime() {
		return 500;
	}

	@Override public void channelUpdated(Channel channel, Optional<?> newValue) {
		triggerInitialize();
	}

	@Override public void dispose() {

	}

	@Override public ModbusTransaction getTransaction() throws OpenemsModbusException {
		TCPMasterConnection connection = getModbusConnection();
		ModbusTCPTransaction trans = new ModbusTCPTransaction(connection);
		return trans;
	}

	@Override public void addDevice(Device device) {
		super.addDevice(device);
		triggerInitialize();
	}

	@Override public String toString() {
		return "ModbusTcp [ip=" + ip + "]";
	}

	@Override protected boolean initialize() {
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

	@Override protected void closeModbusConnection() {
		if (connection.isPresent() && connection.get().isConnected()) {
			try {
				connection.get().close();
			} catch (NullPointerException e) { /* ignore */}
		}
		connection = Optional.empty();
	}

	private TCPMasterConnection getModbusConnection() throws OpenemsModbusException {
		if (!connection.isPresent()) {
			try {
				connection = Optional.of(new TCPMasterConnection(ip.value()));
				connection.get().setPort(port.value());
			} catch (InvalidValueException e) {
				throw new OpenemsModbusException("Modbus-TCP is not configured completely");
			}
		}
		if (!connection.get().isConnected()) {
			try {
				connection.get().connect();
			} catch (Exception e) {
				throw new OpenemsModbusException("Unable to open Modbus-TCP connection: " + ip.valueOptional().get());
			}
		}
		return connection.get();
	}
}
