package io.openems.impl.device.system;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Enumeration;

import io.openems.api.channel.ReadChannel;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.ConfigException;
import io.openems.impl.protocol.system.SystemDeviceNature;
import io.openems.impl.protocol.system.SystemReadChannel;

@ThingInfo("System information")
public class SystemNature extends SystemDeviceNature implements io.openems.api.device.nature.system.SystemNature {

	private final Inet4Address[] OPENEMS_STATIC_IPS;

	public SystemNature(String thingId) throws ConfigException {
		super(thingId);
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
}
