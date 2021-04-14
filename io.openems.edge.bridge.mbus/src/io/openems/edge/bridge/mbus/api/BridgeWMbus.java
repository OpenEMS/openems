package io.openems.edge.bridge.mbus.api;

public interface BridgeWMbus {

	public void addProtocol(String sourceId, WMbusProtocol protocol);

	public void removeProtocol(String sourceId);
}
