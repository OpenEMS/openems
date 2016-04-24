package de.fenecon.femscore.modbus.protocol;

import net.wimpi.modbus.procimg.Register;

public class UnsignedIntegerElement extends NumberElement<Long> {
	public UnsignedIntegerElement(String name, int length, int multiplier, int delta, String unit) {
		super(name, length, multiplier, delta, unit);
	}

	@Override
	public Long _convert(Register register) {
		return ModbusUtils.registerTo16UInt(register) * multiplier - delta;
	}

	@Override
	protected Long _convert(Register[] registers) {
		if (registers.length > 2) {
			throw new UnsupportedOperationException("More than 2 registers not implemented!");
		}
		return ModbusUtils.registersTo32UInt(registers[0], registers[1]) * multiplier - delta;
	}
}
