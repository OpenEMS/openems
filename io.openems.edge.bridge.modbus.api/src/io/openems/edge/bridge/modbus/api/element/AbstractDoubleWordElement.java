package io.openems.edge.bridge.modbus.api.element;

import io.openems.common.types.OpenemsType;

public abstract class AbstractDoubleWordElement extends AbstractModbusRegisterElement<Long> {

	public AbstractDoubleWordElement(int startAddress) {
		super(OpenemsType.LONG, startAddress);
	}

	@Override
	public final int getLength() {
		return 2;
	}
}
