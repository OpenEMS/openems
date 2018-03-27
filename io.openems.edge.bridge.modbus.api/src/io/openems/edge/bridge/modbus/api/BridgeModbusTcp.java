package io.openems.edge.bridge.modbus.api;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.edge.bridge.modbus.protocol.ModbusProtocol;

@ProviderType
public interface BridgeModbusTcp {

	public void addProtocol(String sourceId, Integer unitId, ModbusProtocol protocol);

	public void removeProtocol(String sourceId);

}
