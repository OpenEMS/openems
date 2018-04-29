package io.openems.edge.bridge.modbus.api;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.edge.common.component.OpenemsComponent;

@ProviderType
public interface BridgeModbus extends OpenemsComponent {

	public void addProtocol(String sourceId, ModbusProtocol protocol);

	public void removeProtocol(String sourceId);

}
