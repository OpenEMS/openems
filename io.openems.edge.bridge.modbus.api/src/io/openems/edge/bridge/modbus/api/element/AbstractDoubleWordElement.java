package io.openems.edge.bridge.modbus.api.element;

import io.openems.common.types.OpenemsType;

public abstract class AbstractDoubleWordElement<T> extends AbstractModbusRegisterElement<T> {

	public AbstractDoubleWordElement(OpenemsType type, int startAddress) {
		super(type, startAddress);
	}

	@Override
	public final int getLength() {
		return 2;
	}
}
