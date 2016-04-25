package de.fenecon.femscore.modbus.protocol;

import java.util.List;

import net.wimpi.modbus.procimg.Register;

public class SignedIntegerElement extends NumberElement<Integer> {
	final boolean littleEndian;

	public SignedIntegerElement(int address, String name, int length, int multiplier, int delta, String unit,
			boolean littleEndian) {
		super(address, name, length, multiplier, delta, unit);
		this.littleEndian = littleEndian;
	}

	@Override
	protected Integer convert(Register register) {
		return ModbusUtils.registerTo16Int(register) * multiplier - delta;
	}

	@Override
	protected Integer convert(List<Register> registers) {
		if (registers.size() > 2) {
			throw new UnsupportedOperationException("More than 2 registers not implemented!");
		}
		return ModbusUtils.registersTo32Int(registers.get(0), registers.get(1)) * multiplier - delta;
	}
}
