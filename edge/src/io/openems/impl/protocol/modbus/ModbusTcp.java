/*******************************************************************************
 * OpenEMS - Open Source Energy Management System
 * Copyright (c) 2016, 2017 FENECON GmbH and contributors
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
import io.openems.api.doc.ChannelInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.OpenemsModbusException;

@ThingInfo(title = "Modbus/TCP")
public class ModbusTcp extends ModbusBridge {

	private final ChannelUpdateListener channelUpdateListener = new ChannelUpdateListener() {
		@Override
		public void channelUpdated(Channel channel, Optional<?> newValue) {
			triggerInitialize();
		}
	};

	/*
	 * Config
	 */
	@ChannelInfo(title = "IP address", description = "Sets the IP address (e.g. 10.0.0.15).", type = Inet4Address.class)
	public final ConfigChannel<Inet4Address> ip = new ConfigChannel<Inet4Address>("ip", this)
			.addUpdateListener(channelUpdateListener);

	@ChannelInfo(title = "Port", description = "Sets the port (e.g. 502).", type = Integer.class, defaultValue = "502")
	public final ConfigChannel<Integer> port = new ConfigChannel<Integer>("port", this)
			.addUpdateListener(channelUpdateListener);

	/*
	 * Fields
	 */

	private static Logger log = LoggerFactory.getLogger(ModbusTcp.class);
	private Optional<TCPMasterConnection> connection = Optional.empty();

	/*
	 * Methods
	 */

	@Override
	public void dispose() {

	}

	@Override
	public ModbusTransaction getTransaction() throws OpenemsModbusException {
		TCPMasterConnection connection = getModbusConnection();
		ModbusTCPTransaction trans = new ModbusTCPTransaction(connection);
		return trans;
	}

	@Override
	public void addDevice(Device device) {
		super.addDevice(device);
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
			try {
				connection.get().close();
			} catch (NullPointerException e) { /* ignore */}
		}
		connection = Optional.empty();
	}

	private TCPMasterConnection getModbusConnection() throws OpenemsModbusException {
		if (!connection.isPresent()) {
			try {
				TCPMasterConnection tcpCon = new TCPMasterConnection(ip.value());
				tcpCon.setPort(port.valueOptional().orElse(502));
				connection = Optional.of(tcpCon);
			} catch (InvalidValueException e) {
				throw new OpenemsModbusException("Modbus-TCP is not configured completely");
			}
		}
		if (!connection.get().isConnected()) {
			try {
				TCPMasterConnection tcpCon = connection.get();
				tcpCon.connect();
				tcpCon.getModbusTransport().setTimeout(1000);
			} catch (Exception e) {
				throw new OpenemsModbusException("Unable to open Modbus-TCP connection: " + ip.valueOptional().get());
			}
		}
		return connection.get();
	}
}
