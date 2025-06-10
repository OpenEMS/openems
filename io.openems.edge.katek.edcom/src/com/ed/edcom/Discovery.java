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

import java.io.IOException;
import java.net.InetAddress;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

/**
 * Local network search utilities (mDNS)
 */
public final class Discovery implements ServiceListener {

	private static final String SERVICE_TYPE = "_centurio._tcp.local.";

	private static Discovery instance;
	private final JmDNS mdnsService;
	private ServiceInfo sInfo[] = null;

	private Discovery(InetAddress ia) throws IOException {
		mdnsService = JmDNS.create(ia);
		mdnsService.addServiceListener(SERVICE_TYPE, this);
	}

	/**
	 * Factory method for search utilities
	 *
	 * @param ia current host address
	 * @return new or already existing instance of Discovery class
	 * @throws java.io.IOException some exception
	 */
	public static synchronized Discovery getInstance(InetAddress ia) throws IOException {
		if (instance == null) {
			instance = new Discovery(ia);
		}
		return instance;
	}

	/**
	 * Get inverters found
	 *
	 * @return inverters list
	 */
	public ServiceInfo[] refreshInverterList() {
		sInfo = mdnsService.list(SERVICE_TYPE);
		return sInfo;
	}

	/**
	 * Get inverter info by name
	 *
	 * @param inverterName inverter name (MAC Address as string)
	 * @return service info or 'null' if no inverter found
	 */
	public ServiceInfo getByMac(String inverterName) {
		ServiceInfo sr = null;
		if (sInfo == null) {
			sInfo = mdnsService.list(SERVICE_TYPE);
		}
		sInfo = mdnsService.list(SERVICE_TYPE);
		String fname = inverterName.replaceAll("-", "");
		String lcname = fname.toLowerCase();
		for (ServiceInfo s : sInfo) {
			if (s.getName().contains(fname) || s.getName().contains(lcname)) {
				sr = s;
				break;
			}
		}
		return sr;
	}

	/**
	 * Get inverter info by serial number
	 *
	 * @param serialNum inverter serial number
	 * @return service info or 'null' if no inverter found
	 */
	@SuppressWarnings("deprecation")
	public ServiceInfo getBySerialNumber(String serialNum) {
		ServiceInfo sr = null;
		if (sInfo == null) {
			sInfo = mdnsService.list(SERVICE_TYPE);
		}
		sInfo = mdnsService.list(SERVICE_TYPE);
		for (ServiceInfo s : sInfo) {
			if (s.getTextString().contains(serialNum)) {
				sr = s;
				break;
			}
		}
		return sr;
	}

	/**
	 * Close mDNS service discovery
	 *
	 * @throws IOException some exception
	 */
	public void close() throws IOException {
		mdnsService.close();
		instance = null;
	}

	@Override
	public void serviceAdded(ServiceEvent event) {

	}

	@Override
	public void serviceRemoved(ServiceEvent event) {

	}

	@Override
	public void serviceResolved(ServiceEvent event) {

	}
}
//CHECKSTYLE:ON
