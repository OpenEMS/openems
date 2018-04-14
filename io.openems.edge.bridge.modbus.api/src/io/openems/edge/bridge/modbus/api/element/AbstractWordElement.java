package io.openems.edge.bridge.modbus.api.element;

public abstract class AbstractWordElement extends AbstractModbusRegisterElement<Integer> {

	public AbstractWordElement(int startAddress) {
		super(startAddress);
	}

	@Override
	public final int getLength() {
		return 1;
	}
}
