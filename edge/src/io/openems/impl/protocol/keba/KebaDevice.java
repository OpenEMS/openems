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
package io.openems.impl.protocol.keba;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import io.openems.api.bridge.Bridge;
import io.openems.api.bridge.BridgeReadTask;
import io.openems.api.bridge.BridgeWriteTask;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.device.Device;
import io.openems.api.device.nature.DeviceNature;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.exception.ConfigException;
import io.openems.common.exceptions.OpenemsException;

public abstract class KebaDevice extends Device {

	/*
	 * Constructors
	 */
	public KebaDevice(Bridge parent) throws OpenemsException {
		super(parent);
	}

	/*
	 * Fields
	 */
	private final static int PORT = 7090;

	/*
	 * Config
	 */
	@ChannelInfo(title = "IP address", description = "Sets the IP address (e.g. 192.168.25.11).", type = Inet4Address.class)
	public final ConfigChannel<Inet4Address> ip = new ConfigChannel<Inet4Address>("ip", this);

	/**
	 * Send UDP message to Keba EVCS
	 *
	 * @param s
	 * @throws IOException
	 * @throws ConfigException
	 * @throws InterruptedException
	 */
	protected void send(String s) throws OpenemsException {
		Optional<Inet4Address> ipOpt = this.ip.valueOptional();
		if (!ipOpt.isPresent()) {
			throw new ConfigException("No IP address configured for Device[" + this.id() + "]");
		} else {
			Inet4Address ip = ipOpt.get();
			byte[] raw = s.getBytes();
			DatagramPacket packet = new DatagramPacket(raw, raw.length, ip, PORT);
			DatagramSocket dSocket = null;
			try {
				dSocket = new DatagramSocket();
				log.info("Sending message to KEBA [" + s + "]");
				dSocket.send(packet);
			} catch (SocketException e) {
				throw new OpenemsException("Unable to open UDP socket for sending [" + s + "] to ["
						+ ip.getHostAddress() + "]: " + e.getMessage(), e);
			} catch (IOException e) {
				throw new OpenemsException(
						"Unable to send [" + s + "] UDP message to [" + ip.getHostAddress() + "]: " + e.getMessage());

			} finally {
				if (dSocket != null) {
					dSocket.close();
				}
			}
		}
	}

	@Override
	public List<BridgeReadTask> getReadTasks() {
		List<BridgeReadTask> readTasks = new ArrayList<>();
		for (DeviceNature nature : getDeviceNatures()) {
			for (BridgeReadTask task : nature.getReadTasks()) {
				readTasks.add(task);
			}
		}
		return readTasks;
	}

	@Override
	public List<BridgeWriteTask> getWriteTasks() {
		List<BridgeWriteTask> writeTasks = new ArrayList<>();
		for (DeviceNature nature : getDeviceNatures()) {
			for (BridgeWriteTask task : nature.getWriteTasks()) {
				writeTasks.add(task);
			}
		}
		return writeTasks;
	}

	/**
	 * Forward message to deviceNature
	 */
	public void receive(String message) {
		for (DeviceNature nature : this.getDeviceNatures()) {
			if (nature instanceof KebaDeviceNature) {
				KebaDeviceNature kebaNature = (KebaDeviceNature) nature;
				kebaNature.receive(message);
			}
		}
	}
}
