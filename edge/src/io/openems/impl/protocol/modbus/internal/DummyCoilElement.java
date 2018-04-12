package io.openems.impl.protocol.modbus.internal;

public class DummyCoilElement extends CoilElement {

	private final int length;

	public DummyCoilElement(int address) {
		this(address, address);
	}

	public DummyCoilElement(int fromAddress, int toAddress) {
		super(fromAddress, null);
		this.length = toAddress - fromAddress + 1;
	}

	@Override public int getLength() {
		return length;
	}

	/**
	 * We are not setting a value for a DummyElement.
	 */
	@Override
	public void setValue(Boolean value) {
		return;
	}









}
