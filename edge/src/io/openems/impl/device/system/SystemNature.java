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
package io.openems.impl.device.system;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

import io.openems.api.channel.ReadChannel;
import io.openems.api.channel.StaticValueChannel;
import io.openems.api.device.Device;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.ConfigException;
import io.openems.api.thing.ThingChannelsUpdatedListener;
import io.openems.impl.protocol.system.SystemDeviceNature;
import io.openems.impl.protocol.system.SystemReadChannel;

@ThingInfo(title = "Operating system")
public class SystemNature extends SystemDeviceNature implements io.openems.api.device.nature.system.SystemNature {

	/*
	 * Constructors
	 */
	public SystemNature(String thingId, Device parent) throws ConfigException {
		super(thingId, parent);
		try {
			OPENEMS_STATIC_IPS = new Inet4Address[] { //
					// 192.168.100.100
					(Inet4Address) InetAddress
							.getByAddress(new byte[] { (byte) 192, (byte) 168, (byte) 100, (byte) 100 }),
					// 10.4.0.1
					(Inet4Address) InetAddress.getByAddress(new byte[] { (byte) 10, (byte) 4, (byte) 0, (byte) 1 }) };
		} catch (UnknownHostException e) {
			throw new ConfigException("Error initializing OpenEMS Static IP: " + e.getMessage());
		}
	}

	/*
	 * Inherited Channels
	 */
	private SystemReadChannel<Inet4Address> primaryIpAddress = new SystemReadChannel<Inet4Address>("PrimaryIpAddress",
			this);

	@Override
	public ReadChannel<Inet4Address> primaryIpAddress() {
		return primaryIpAddress;
	}

	private StaticValueChannel<Integer> openemsVersionMajor = new StaticValueChannel<Integer>("OpenemsVersionMajor",
			this, 1);
	// TODO https://stackoverflow.com/questions/2712970/get-maven-artifact-version-at-runtime

	@Override
	public ReadChannel<Integer> openemsVersionMajor() {
		return openemsVersionMajor;
	}

	/*
	 * Fields
	 */
	private final Inet4Address[] OPENEMS_STATIC_IPS;
	private List<ThingChannelsUpdatedListener> listeners = new ArrayList<>();;

	/*
	 * Methods
	 */
	@Override
	public void addListener(ThingChannelsUpdatedListener listener) {
		this.listeners.add(listener);
	}

	@Override
	public void removeListener(ThingChannelsUpdatedListener listener) {
		this.listeners.remove(listener);
	}

	@Override
	protected void update() {
		// Get IP address
		Inet4Address primaryIpAddress;
		try {
			primaryIpAddress = getPrimaryIpAddress();
			if (primaryIpAddress != null) {
				this.primaryIpAddress.updateValue(primaryIpAddress);
			}
		} catch (SocketException e) {
			log.error("Error getting primary IPv4 address: " + e.getMessage());
		}

	}

	private Inet4Address getPrimaryIpAddress() throws SocketException {
		Inet4Address primaryIpAddress = null;
		boolean foundOpenEmsStaticIp = false;
		Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
		while (e.hasMoreElements()) {
			NetworkInterface n = e.nextElement();
			Enumeration<InetAddress> ee = n.getInetAddresses();
			while (ee.hasMoreElements()) {
				InetAddress i = ee.nextElement();
				if (i instanceof Inet4Address) {
					Inet4Address inetAddress = (Inet4Address) i;
					if (inetAddress.isLinkLocalAddress() || inetAddress.isLoopbackAddress()) {
						// ignore local and loopback address
						continue;
					} else if (Arrays.asList(OPENEMS_STATIC_IPS).contains(inetAddress)) {
						// ignore static ip
						foundOpenEmsStaticIp = true;
						continue;
					} else {
						// take this ip and stop
						primaryIpAddress = inetAddress;
					}
				}
			}
		}
		if (primaryIpAddress == null && foundOpenEmsStaticIp) {
			// set static ip if no other ip was found and it is existing
			primaryIpAddress = OPENEMS_STATIC_IPS[0];
		}
		return primaryIpAddress;
	}

	@Override
	public void init() {
		for (ThingChannelsUpdatedListener listener : this.listeners) {
			listener.thingChannelsUpdated(this);
		}
	}
}
