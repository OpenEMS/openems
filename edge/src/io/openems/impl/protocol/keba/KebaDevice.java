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
import java.util.List;
import java.util.Optional;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.device.Device;
import io.openems.api.device.nature.DeviceNature;
import io.openems.api.doc.ConfigInfo;
import io.openems.api.exception.ConfigException;
import io.openems.api.exception.OpenemsException;

public abstract class KebaDevice extends Device {

	/*
	 * Constructors
	 */
	public KebaDevice() throws OpenemsException {
		super();
		log.info("Constructor KebaDevice");
	}

	/*
	 * Fields
	 */
	private final int REPORT_DELAY = 2000;

	/*
	 * Config
	 */
	@ConfigInfo(title = "IP address", description = "Sets the IP address (e.g. 192.168.25.11).", type = Inet4Address.class)
	public final ConfigChannel<Inet4Address> ip = new ConfigChannel<Inet4Address>("ip", this);

	@ConfigInfo(title = "Port", description = "Sets the port (e.g. 7090).", type = Integer.class, defaultValue = "7090")
	public final ConfigChannel<Integer> port = new ConfigChannel<Integer>("port", this);

	protected void update() throws InterruptedException {
		log.info("Update");
		try {
			this.send("report 1");
			this.send("report 2");
			this.send("report 3");
		} catch (ConfigException | IOException e) {
			log.error("Error while updating KebaDevice [" + this.id() + "]: " + e.getMessage());
		}
	}

	/**
	 * Send UDP message to Keba EVCS
	 *
	 * @param s
	 * @throws IOException
	 * @throws ConfigException
	 * @throws InterruptedException
	 */
	protected void send(String s) throws IOException, ConfigException, InterruptedException {
		Optional<Inet4Address> ip = this.ip.valueOptional();
		Optional<Integer> port = this.port.valueOptional();
		if (!ip.isPresent() || !port.isPresent()) {
			throw new ConfigException(
					"No ip [" + ip + "] or port [" + port + "] configured for Device[" + this.id() + "]");
		} else {
			byte[] raw = s.getBytes();

			DatagramPacket packet = new DatagramPacket(raw, raw.length, ip.get(), port.get());
			DatagramSocket dSocket = new DatagramSocket();
			dSocket.send(packet);
			// dSocket.close();
			log.info("Sent...");
			Thread.sleep(REPORT_DELAY);
		}
	}

	protected void write() throws InterruptedException {
		for (DeviceNature nature : getDeviceNatures()) {
			if (nature instanceof KebaDeviceNature) {
				List<String> messages = ((KebaDeviceNature) nature).getWriteMessages();
				try {
					for (String message : messages) {
						this.send(message);
					}
				} catch (ConfigException | IOException e) {
					log.error("Error while writing [{}] to KebaDevice [{}]: {}", String.join(",", messages), this.id(),
							e.getMessage());
				}
			}
		}
	}
}
