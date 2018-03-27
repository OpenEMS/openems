package io.openems.edge.bridge.modbus.protocol;

public class RegisterRange extends Range {

	public RegisterRange(int startAddress, RegisterElement<?>... elements) {
		super(startAddress, elements);
	}

}
