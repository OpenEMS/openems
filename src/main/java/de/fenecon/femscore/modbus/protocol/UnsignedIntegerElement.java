package de.fenecon.femscore.modbus.protocol;

import java.util.List;

import net.wimpi.modbus.procimg.Register;

public class UnsignedIntegerElement extends NumberElement<Long> {
	public UnsignedIntegerElement(int address, String name, int length, int multiplier, int delta, String unit) {
		super(address, name, length, multiplier, delta, unit);
	}

	@Override
	public Long convert(Register register) {
		return ModbusUtils.registerTo16UInt(register) * multiplier - delta;
	}

	@Override
	protected Long convert(List<Register> registers) {
		if (registers.size() > 2) {
			throw new UnsupportedOperationException("More than 2 registers not implemented!");
		}
		return ModbusUtils.registersTo32UInt(registers.get(0), registers.get(1)) * multiplier - delta;
	}
}
