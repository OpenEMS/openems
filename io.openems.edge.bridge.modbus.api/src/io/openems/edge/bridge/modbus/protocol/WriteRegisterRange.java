package io.openems.edge.bridge.modbus.protocol;

public class WriteRegisterRange extends RegisterRange implements WriteRange {

	public WriteRegisterRange(int startAddress, RegisterElement<?>... elements) {
		super(startAddress, elements);
	}

}
