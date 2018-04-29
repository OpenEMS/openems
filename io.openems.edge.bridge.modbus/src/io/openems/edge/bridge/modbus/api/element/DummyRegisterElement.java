package io.openems.edge.bridge.modbus.api.element;

import java.util.Optional;

import com.ghgande.j2mod.modbus.procimg.InputRegister;
import com.ghgande.j2mod.modbus.procimg.Register;

import io.openems.common.types.OpenemsType;

public class DummyRegisterElement extends AbstractModbusElement<Void> implements ModbusRegisterElement<Void> {

	private final int length;

	public DummyRegisterElement(int address) {
		this(address, address);
	}

	public DummyRegisterElement(int fromAddress, int toAddress) {
		super(OpenemsType.INTEGER /* does not matter */, fromAddress);
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
	public void setInputRegisters(InputRegister... registers) {
		return;
	}

	@Override
	@Deprecated
	public void _setNextWriteValue(Optional<Void> valueOpt) {
		// ignore write
		return;
	}

	@Override
	public Optional<Register[]> getNextWriteValue() {
		return Optional.empty();
	}
}
