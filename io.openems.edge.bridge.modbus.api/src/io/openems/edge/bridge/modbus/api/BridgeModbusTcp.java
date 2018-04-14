package io.openems.edge.bridge.modbus.api;

import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public interface BridgeModbusTcp {

	public void addProtocol(String sourceId, ModbusProtocol protocol);

	public void removeProtocol(String sourceId);

}
