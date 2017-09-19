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

import java.util.Optional;

import com.ghgande.j2mod.modbus.Modbus;
import com.ghgande.j2mod.modbus.io.ModbusSerialTransaction;
import com.ghgande.j2mod.modbus.io.ModbusTransaction;
import com.ghgande.j2mod.modbus.net.SerialConnection;
import com.ghgande.j2mod.modbus.util.SerialParameters;

import io.openems.api.channel.Channel;
import io.openems.api.channel.ChannelUpdateListener;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.device.Device;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.OpenemsModbusException;

@ThingInfo(title = "Modbus/RTU")
public class ModbusRtu extends ModbusBridge {

	private final ChannelUpdateListener channelUpdateListener = new ChannelUpdateListener() {
		@Override
		public void channelUpdated(Channel channel, Optional<?> newValue) {
			triggerInitialize();
		}
	};

	/*
	 * Config
	 */
	@ChannelInfo(title = "Baudrate", description = "Sets the baudrate (e.g. 9600).", type = Integer.class)
	public final ConfigChannel<Integer> baudrate = new ConfigChannel<Integer>("baudrate", this)
			.addUpdateListener(channelUpdateListener);

	@ChannelInfo(title = "Databits", description = "Sets the databits (e.g. 8).", type = Integer.class)
	public final ConfigChannel<Integer> databits = new ConfigChannel<Integer>("databits", this)
			.addUpdateListener(channelUpdateListener);

	@ChannelInfo(title = "Parity", description = "Sets the parity (e.g. 'even').", type = String.class)
	public final ConfigChannel<String> parity = new ConfigChannel<String>("parity", this)
			.addUpdateListener(channelUpdateListener);

	@ChannelInfo(title = "Serial interface", description = "Sets the serial interface (e.g. /dev/ttyUSB0).", type = String.class)
	public final ConfigChannel<String> serialinterface = new ConfigChannel<String>("serialinterface", this)
			.addUpdateListener(channelUpdateListener);

	@ChannelInfo(title = "Stopbits", description = "Sets the stopbits (e.g. 1).", type = Integer.class)
	public final ConfigChannel<Integer> stopbits = new ConfigChannel<Integer>("stopbits", this)
			.addUpdateListener(channelUpdateListener);

	/*
	 * Fields
	 */
	private Optional<SerialConnection> connection = Optional.empty();

	/*
	 * Methods
	 */
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

	@Override
	public void addDevice(Device device) {
		super.addDevice(device);
		triggerInitialize();
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
			try {
				connection.get().close();
			} catch (NullPointerException e) { /* ignore */}
		}
		connection = Optional.empty();
	}

	private SerialConnection getModbusConnection() throws OpenemsModbusException {
		if (!connection.isPresent()) {
			if (!baudrate.valueOptional().isPresent() || !databits.valueOptional().isPresent()
					|| !parity.valueOptional().isPresent() || !serialinterface.valueOptional().isPresent()
					|| !stopbits.valueOptional().isPresent()) {
				throw new OpenemsModbusException("Modbus-RTU is not configured completely");
			}
			SerialParameters params = new SerialParameters();
			params.setPortName(serialinterface.valueOptional().get());
			params.setBaudRate(baudrate.valueOptional().get());
			params.setDatabits(databits.valueOptional().get());
			params.setParity(parity.valueOptional().get());
			params.setStopbits(stopbits.valueOptional().get());
			params.setEncoding(Modbus.SERIAL_ENCODING_RTU);
			params.setEcho(false);
			connection = Optional.of(new SerialConnection(params));
		}
		if (!connection.get().isOpen()) {
			try {
				SerialConnection serialCon = connection.get();
				serialCon.open();
				serialCon.getModbusTransport().setTimeout(1000);
			} catch (Exception e) {
				throw new OpenemsModbusException("Unable to open Modbus-RTU connection: " + connection);
			}
		}
		return connection.get();
	}

}
