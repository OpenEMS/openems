package io.openems.edge.bridge.modbus.api.element;

public abstract class AbstractDoubleWordElement extends AbstractModbusRegisterElement<Long> {

	public AbstractDoubleWordElement(int startAddress) {
		super(startAddress);
	}

	@Override
	public final int getLength() {
		return 2;
	}
}
