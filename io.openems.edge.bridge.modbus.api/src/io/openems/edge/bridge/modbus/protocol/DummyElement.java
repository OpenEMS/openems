package io.openems.edge.bridge.modbus.protocol;

import com.ghgande.j2mod.modbus.procimg.InputRegister;

public class DummyElement extends RegisterElement<Void> {

	private final int length;

	public DummyElement(int address) {
		this(address, address);
	}

	public DummyElement(int fromAddress, int toAddress) {
		super(fromAddress);
		this.length = toAddress - fromAddress + 1;
	}

	@Override
	public int getLength() {
		return length;
	}

	/**
	 * We are not setting a value for a DummyElement.
	 */
	@Override
	protected void _setInputRegisters(InputRegister... registers) {
		return;
	}
}
