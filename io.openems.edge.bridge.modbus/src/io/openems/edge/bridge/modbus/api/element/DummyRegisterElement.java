package io.openems.edge.bridge.modbus.api.element;

import java.nio.ByteBuffer;
import java.util.Optional;

import com.ghgande.j2mod.modbus.procimg.InputRegister;
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

	/**
	 * We are not setting a value for a DummyElement.
	 */
//	@Override
//	public void setInputRegisters(InputRegister... registers) {
//	}

//	@Override
//	protected void _setInputRegisters(InputRegister... registers) {
//	}

//	@Override
//	@Deprecated
//	public void _setNextWriteValue(Optional<Void> valueOpt) {
//		// ignore write
//		this.onSetNextWriteCallbacks.forEach(callback -> callback.accept(valueOpt));
//	}

	@Override
	public Optional<Register[]> getNextWriteValue() {
		return Optional.empty();
	}

	@Override
	protected DummyRegisterElement self() {
		return this;
	}

	@Override
	protected Void convert(ByteBuffer buff) {
		return null;
	}

	@Override
	protected InputRegister[] valueToBinary(Void value) {
		return new InputRegister[length];
	}

}
