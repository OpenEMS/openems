package io.openems.edge.bridge.modbus.api.element;

import com.ghgande.j2mod.modbus.procimg.Register;

import io.openems.common.types.OpenemsType;

/**
 * A WordElement has a size of one Modbus Registers or 16 bit.
 *
 * @param <SELF> the subclass of myself
 * @param <T>    the OpenEMS type
 */
public abstract class AbstractSingleWordElement<SELF extends ModbusRegisterElement<SELF, T>, T>
		extends ModbusRegisterElement<SELF, T> {

	public AbstractSingleWordElement(OpenemsType type, int startAddress) {
		super(type, startAddress, 1);
	}

	@Override
	protected final T rawToValue(Register[] registers) {
		return this.rawToValue(registers, WordOrder.MSWLSW /* makes no difference for SingleWord */);
	}

	@Override
	protected Register[] valueToRaw(T value) {
		return this.valueToRaw(value, WordOrder.MSWLSW /* makes no difference for SingleWord */);
	}

}