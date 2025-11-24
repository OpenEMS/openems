package io.openems.edge.bridge.onewire.impl;

import org.openmuc.jeebus.ship.api.ConnectionHandler;
import org.openmuc.jeebus.ship.api.DisconnectReason;
import org.openmuc.jeebus.ship.api.ShipConnectionInterface;

// See: https://www.openmuc.org/eebus/ship/user-guide/
public class EebusConnectionHandler implements ConnectionHandler {

	@Override
	public void onMessageReceived(byte[] fullMsg, byte[] payload, ShipConnectionInterface shipConn) {
		System.out.println("EebusConnectionHandler onMessageReceived");
	}

	@Override
	public void onDisconnect(DisconnectReason reason, ShipConnectionInterface shipConn) {
		System.out.println("EebusConnectionHandler onDisconnect: reason=" + reason.name());
	}

	@Override
	public void serviceAdded(String ipAddr, String ski) {
		System.out.println("EebusConnectionHandler serviceAdded: ipAddr=" + ipAddr + "; ski=" + ski);
	}

	@Override
	public void serviceRemoved(String ipAddr) {
		System.out.println("EebusConnectionHandler serviceRemoved: ipAddr=" + ipAddr);
	}

	@Override
	public void connectionDataExchangeEnabled(String ipAddr) {
		System.out.println("EebusConnectionHandler connectionDataExchangeEnabled: ipAddr=" + ipAddr);
	}
}
