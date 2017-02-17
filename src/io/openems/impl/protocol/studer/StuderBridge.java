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
package io.openems.impl.protocol.studer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import io.openems.api.bridge.Bridge;
import io.openems.api.channel.Channel;
import io.openems.api.channel.ChannelUpdateListener;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.device.Device;
import io.openems.api.doc.ConfigInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.OpenemsException;
import io.openems.impl.protocol.studer.internal.Request;
import io.openems.impl.protocol.studer.internal.StuderConnection;
import io.openems.impl.protocol.studer.internal.request.ReadRequest;
import io.openems.impl.protocol.studer.internal.request.ReadResponse;

@ThingInfo(title = "Studer")
public class StuderBridge extends Bridge implements ChannelUpdateListener {

	private final ChannelUpdateListener channelUpdateListener = new ChannelUpdateListener() {
		@Override
		public void channelUpdated(Channel channel, Optional<?> newValue) {
			triggerInitialize();
		}
	};

	/*
	 * Config
	 */
	@ConfigInfo(title = "Serial interface", description = "Sets the serial interface (e.g. /dev/ttyUSB0).", type = String.class)
	public final ConfigChannel<String> serialinterface = new ConfigChannel<String>("serialinterface", this)
			.addUpdateListener(channelUpdateListener);

	@ConfigInfo(title = "Source address", description = "Sets the source address (e.g. 1).", type = Integer.class, defaultValue = "1")
	public final ConfigChannel<Integer> address = new ConfigChannel<Integer>("address", this);

	private ConfigChannel<Integer> cycleTime = new ConfigChannel<Integer>("cycleTime", this).defaultValue(1000);

	@Override
	public ConfigChannel<Integer> cycleTime() {
		return cycleTime;
	}

	/*
	 * Fields
	 */
	private Optional<StuderConnection> connection = Optional.empty();
	protected volatile StuderDevice[] studerdevices = new StuderDevice[0];
	private AtomicBoolean isWriteTriggered = new AtomicBoolean(false);

	/*
	 * Methods
	 */
	@Override
	public void dispose() {
		this.closeConnection();
	}

	@Override
	public void channelUpdated(Channel channel, Optional<?> newValue) {
		triggerInitialize();
	}

	@Override
	public void triggerWrite() {
		// set the Write-flag
		isWriteTriggered.set(true);
		// start "run()" again as fast as possible
		triggerForceRun();
	}

	@Override
	protected void forever() {
		for (StuderDevice studerdevice : studerdevices) {
			// if Write-flag was set -> start writing for all Devices immediately
			if (isWriteTriggered.get()) {
				isWriteTriggered.set(false);
				writeAllDevices();
			}
			// Update this Device
			try {
				studerdevice.update(this);
			} catch (OpenemsException e) {
				log.error(e.getMessage());
			}
		}
	}

	/**
	 * Starts writing at all StuderDevices
	 */
	private void writeAllDevices() {
		for (StuderDevice studerdevice : studerdevices) {
			try {
				studerdevice.write(this);
			} catch (OpenemsException e) {
				log.error("Error while writing to StuderDevice [" + studerdevice.id() + "]: " + e.getMessage());
			}
		}
	}

	/**
	 * Gets a serial connection. Tries to establish one if there is none yet.
	 *
	 * @return
	 * @throws OpenemsException
	 */
	public StuderConnection getConnection() throws OpenemsException {
		if (!connection.isPresent()) {
			if (!serialinterface.valueOptional().isPresent()) {
				throw new OpenemsException("StuderBridge is not configured completely");
			}
			connection = Optional.of(new StuderConnection(serialinterface.valueOptional().get()));
		}
		StuderConnection conn = connection.get();
		if (!conn.isConnected()) {
			conn.connect();
		}
		return conn;
	}

	/**
	 * Closes a serial connection if existing.
	 */
	protected void closeConnection() {
		if (connection.isPresent() && connection.get().isConnected()) {
			try {
				connection.get().disconnect();
			} catch (NullPointerException e) { /* ignore */}
		}
		connection = Optional.empty();
	}

	@Override
	protected boolean initialize() {
		/*
		 * Copy and cast devices to local studerdevices array
		 */
		if (devices.isEmpty()) {
			return false;
		}
		List<StuderDevice> studerdevices = new ArrayList<>();
		for (Device device : devices) {
			if (device instanceof StuderDevice) {
				studerdevices.add((StuderDevice) device);
			}
		}
		StuderDevice[] newStuderdevices = studerdevices.stream().toArray(StuderDevice[]::new);
		if (newStuderdevices == null) {
			newStuderdevices = new StuderDevice[0];
		}
		this.studerdevices = newStuderdevices;
		/*
		 * Create a new SerialConnection
		 */
		closeConnection();
		return true;
	}

	/**
	 * Executes a Request. For a {@link ReadRequest} use getResponse() afterwards to get hold of its
	 * {@link ReadResponse}.
	 *
	 * @param request
	 * @throws IOException
	 * @throws OpenemsException
	 */
	public void execute(Request request) throws IOException, OpenemsException {
		StuderConnection connection = getConnection();
		connection.setRequest(request);
		connection.execute();
	}
}
