package de.fenecon.femscore.modbus.protocol;

import net.wimpi.modbus.procimg.Register;

public class SignedIntegerElement extends NumberElement<Integer> {
	final boolean littleEndian;

	public SignedIntegerElement(String name, int length, int multiplier, int delta, String unit, boolean littleEndian) {
		super(name, length, multiplier, delta, unit);
		this.littleEndian = littleEndian;
	}

	@Override
	protected Integer _convert(Register register) {
		return ModbusUtils.registerTo16Int(register);
	}

	@Override
	protected Integer _convert(Register[] registers) {
		if (registers.length > 2) {
			throw new UnsupportedOperationException("More than 2 registers not implemented!");
		}
		return ModbusUtils.registersTo32Int(registers[0], registers[1]);
	}
}
