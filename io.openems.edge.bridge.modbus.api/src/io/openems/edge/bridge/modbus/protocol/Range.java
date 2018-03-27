package io.openems.edge.bridge.modbus.protocol;

public abstract class Range {

	private RegisterElement<?>[] elements;
	private final int length;
	private final int startAddress;

	public Range(int startAddress, RegisterElement<?>... elements) {
		this.startAddress = startAddress;
		this.elements = elements;
		for (RegisterElement<?> element : elements) {
			element.setModbusRange(this);
		}
		int length = 0;
		for (RegisterElement<?> element : elements) {
			length += element.getLength();
		}
		this.length = length;
	}

	public RegisterElement<?>[] getElements() {
		return elements;
	}

	public int getLength() {
		return length;
	}

	public int getStartAddress() {
		return startAddress;
	}

	@Override
	public String toString() {
		return "Range [startAddress=" + startAddress + ", length=" + length + "]";
	}
}
