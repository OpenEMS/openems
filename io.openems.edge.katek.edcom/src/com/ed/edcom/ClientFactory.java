// CHECKSTYLE:OFF
/*
*   EDCOM 8.1 is a java cross platform library for communication with 10kW
*   hybrid Inverter (Katek Memmingen GmbH).
*   Copyright (C) 2022 Katek Memmingen GmbH
*
*   This program is free software: you can redistribute it and/or modify
*   it under the terms of the GNU Lesser General Public License as published by
*   the Free Software Foundation, either version 3 of the License, or
*   (at your option) any later version.
*
*   This program is distributed in the hope that it will be useful,
*   but WITHOUT ANY WARRANTY; without even the implied warranty of
*   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*   GNU General Public License for more details.
*   
*   You should have received a copy of the GNU Lesser General Public License
*   along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.ed.edcom;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * Client factory.
 */
public final class ClientFactory implements Closeable {

	private final List<Client> list;
	private InetAddress host;

	/**
	 * Empty client factory
	 *
	 * @throws Exception wrong parameters
	 */
	public ClientFactory() throws Exception {
		if (Util.userId < 1) {
			throw new Exception("Library initialization error");
		}
		list = new ArrayList<>();
	}

	/**
	 * Client factory
	 *
	 * @param hostAddress local host IP address related to desired network
	 * @throws Exception wrong parameters
	 */
	public ClientFactory(InetAddress hostAddress) throws Exception {
		if (Util.userId < 1) {
			throw new Exception("Library initialization error");
		}
		list = new ArrayList<Client>();
		this.host = hostAddress;
		byte[] b = hostAddress.getAddress();
		for (int i = 0; i < 256; i++) {
			b[3] = (byte) i;
			InetAddress dip = InetAddress.getByAddress(b);
			list.add(new Client(dip, hostAddress, i * 10));
		}
	}

	/**
	 * Client factory
	 *
	 * @param hostIp local host IP address related to desired network
	 * @param devIp  device IP address
	 * @param delay  start communication after delay [ms]
	 * @return communication object
	 * @throws Exception error
	 */
	public static Client getClient(InetAddress hostIp, InetAddress devIp, int delay) throws Exception {
		return new Client(devIp, hostIp, delay);
	}

	/**
	 * Add new client
	 *
	 * @param cl new client
	 */
	public void addClient(Client cl) {
		list.add(cl);
	}

	/**
	 * Start all clients
	 */
	public void start() {
		for (Client cl : list) {
			try {
				cl.start();
			} catch (Exception e) {
			}
		}
	}

	/**
	 * Closes all clients.
	 *
	 * @throws IOException according to interface definition
	 */
	@Override
	public void close() throws IOException {
		for (Client cl : list) {
			try {
				cl.close();
			} catch (Exception e) {
			}
		}
	}

	/**
	 * Print available TCP/IP interfaces
	 *
	 * @param all true - print all, false - print active interfaces only.
	 */
	public static void printNetworkInterfaces(boolean all) {
		try {
			Enumeration<NetworkInterface> s = NetworkInterface.getNetworkInterfaces();
			System.out.println("-----------------------------------------------------------------");
			System.out.println("Available network interfaces: ");
			while (s.hasMoreElements()) {
				NetworkInterface inf = s.nextElement();
				String sMac = "", ip = "";
				byte[] ba = inf.getHardwareAddress();
				if (ba != null) {
					for (byte b : ba) {
						sMac = sMac.concat(String.format("%02x ", b));
					}
				}
				Enumeration<InetAddress> inetAddresses = inf.getInetAddresses();
				for (InetAddress inetAddress : Collections.list(inetAddresses)) {
					ip = ip + String.format("%s ", inetAddress);
				}
				if (all || inf.isUp()) {
					System.out.println("-----------------------------------------------------------------");
					System.out.println("Name        : " + inf.getName());
					System.out.println("MAC         : " + sMac);
					System.out.println("IP          : " + ip);
					System.out.println("On          : " + inf.isUp());
					System.out.println("Loopback    : " + inf.isLoopback());
					System.out.println("Virtual     : " + inf.isVirtual());
				}
			}
			System.out.println("-----------------------------------------------------------------");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

	/**
	 * Get address for current interface
	 *
	 * @return host IP
	 */
	public InetAddress getHostAddress() {
		return host;
	}

	/**
	 * Get active clients
	 *
	 * @return client list
	 */
	public List<Client> getActiveClients() {
		List<Client> l = new ArrayList<Client>();
		for (Client inv : list) {
			if (inv != null) {
				if (inv.isConnected()) {
					l.add(inv);
				}
			}
		}
		return l;
	}

	/**
	 * Get visible active clients
	 *
	 * @return client list
	 */
	public List<Client> getVisibleActiveClients() {
		List<Client> l = new ArrayList<Client>();
		for (Client inv : list) {
			if (inv != null) {
				if (inv.isVisible()) {
					l.add(inv);
				}
			}
		}
		return l;
	}
}
//CHECKSTYLE:ON
