package io.openems.edge.bridge.mbus.api;

import org.openmuc.jmbus.MBusConnection;

public interface BridgeMbus {

	public void addTask(String sourceId, MbusTask task);

	public MBusConnection getmBusConnection();

	public void removeTask(String sourceId);
}
