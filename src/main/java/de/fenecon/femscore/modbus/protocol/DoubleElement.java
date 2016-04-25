package de.fenecon.femscore.modbus.protocol;

import java.util.List;

import net.wimpi.modbus.procimg.Register;

public class DoubleElement extends NumberElement<Double> {
	public DoubleElement(int address, String name, int length, int multiplier, int delta, String unit) {
		super(address, name, length, multiplier, delta, unit);
	}

	@Override
	protected Double convert(Register register) {
		throw new UnsupportedOperationException("Double is not implemented!");
	}

	@Override
	protected Double convert(List<Register> registers) {
		throw new UnsupportedOperationException("Double is not implemented!");

	}
}
