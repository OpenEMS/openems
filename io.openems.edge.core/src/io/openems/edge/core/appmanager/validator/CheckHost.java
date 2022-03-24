package io.openems.edge.core.appmanager.validator;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class CheckHost implements Checkable {

	private final InetAddress host;
	private final Integer port;

	public CheckHost(String host) {
		this(host, null);
	}

	public CheckHost(String host, Integer port) {
		InetAddress tempIp = null;
		try {
			tempIp = InetAddress.getByName(host);
		} catch (UnknownHostException e) {
			// could not get address by name
		}
		this.host = tempIp;
		this.port = port;
	}

	@Override
	public boolean check() {
		if (this.host == null) {
			return false;
		}
		if (this.port == null) {
			try {
				return this.host.isReachable(1000);
			} catch (IOException e) {
				// not reachable
			}
		} else {
			try {
				// try socket connection on specific port
				var so = new Socket(this.host, this.port);
				so.close();
				return true;
			} catch (IOException e) {
				// not reachable
			}
		}
		return false;
	}

	@Override
	public String getErrorMessage() {
		// TODO translation
		var portMsg = this.port != null ? " on Port " + this.port : "";
		if (this.host == null) {
			return "IP '" + this.host.getHostAddress() + "'" + portMsg + " is not a valid IP-Address";
		}
		return "Device with IP '" + this.host.getHostAddress() + "'" + portMsg + " is not reachable!";
	}

}
