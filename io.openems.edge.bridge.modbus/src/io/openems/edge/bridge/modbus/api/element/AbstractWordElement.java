package io.openems.edge.bridge.modbus.api.element;

import io.openems.common.types.OpenemsType;

public abstract class AbstractWordElement extends AbstractModbusRegisterElement<Integer> {

	public AbstractWordElement(int startAddress) {
		super(OpenemsType.INTEGER, startAddress);
	}

	@Override
	public final int getLength() {
		return 1;
	}
}
