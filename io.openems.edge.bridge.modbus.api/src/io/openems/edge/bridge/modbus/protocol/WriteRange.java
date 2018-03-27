package io.openems.edge.bridge.modbus.protocol;

public interface WriteRange {

	public RegisterElement<?>[] getElements();

	public int getLength();

	public int getStartAddress();

}
