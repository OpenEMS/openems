package io.openems.edge.bridge.modbus.api.task;

import io.openems.edge.bridge.modbus.api.element.ModbusElement;

public interface WriteTask {

	public ModbusElement[] getElements();

	public int getLength();

	public int getStartAddress();

}
