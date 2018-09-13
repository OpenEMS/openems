package io.openems.edge.bridge.modbus.api;

import java.net.InetAddress;

import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public interface BridgeModbusTcp extends BridgeModbus {

	/**
	 * Gets the IP address
	 * 
	 * @return
	 */
	public InetAddress getIpAddress();

}
