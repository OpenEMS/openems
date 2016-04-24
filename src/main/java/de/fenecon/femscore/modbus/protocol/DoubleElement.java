package de.fenecon.femscore.modbus.protocol;

import net.wimpi.modbus.procimg.Register;

public class DoubleElement extends NumberElement<Double> {
	public DoubleElement(String name, int length, int multiplier, int delta, String unit) {
		super(name, length, multiplier, delta, unit);
	}

	@Override
	protected Double _convert(Register register) {
		throw new UnsupportedOperationException("Double is not implemented!");
	}

	@Override
	protected Double _convert(Register[] registers) {
		throw new UnsupportedOperationException("Double is not implemented!");
	}
}
