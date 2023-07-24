package io.openems.edge.bridge.modbus.api.element;

import java.nio.ByteBuffer;

import com.ghgande.j2mod.modbus.procimg.Register;

import io.openems.common.types.OpenemsType;

/**
 * A DummyRegisterElement is a placeholder for an empty
 * {@link ModbusRegisterElement}.
 */
public class DummyRegisterElement extends AbstractMultipleWordsElement<DummyRegisterElement, Void> {

	public DummyRegisterElement(int address) {
		this(address, address);
	}

	public DummyRegisterElement(int fromAddress, int toAddress) {
		super(OpenemsType.INTEGER /* does not matter */, fromAddress, toAddress - fromAddress + 1);
	}

	@Override
	protected DummyRegisterElement self() {
		return this;
	}

	@Override
	protected Register[] valueToRaw(Void value) {
		return new Register[length];
	}

	@Override
	protected Void byteBufferToValue(ByteBuffer buff) {
		return null;
	}

	@Override
	protected void valueToByteBuffer(ByteBuffer buff, Void value) {
		// Nothing
	}

}
