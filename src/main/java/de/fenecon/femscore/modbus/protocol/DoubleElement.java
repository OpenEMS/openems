package de.fenecon.femscore.modbus.protocol;

import com.ghgande.j2mod.modbus.procimg.Register;

public class DoubleElement extends NumberElement<Double> {
	public DoubleElement(int address, int length, String name, short multiplier, short delta, String unit) {
		super(address, length, name, multiplier, delta, unit);
	}

	@Override
	public Register[] toRegister(Double value) {
		throw new UnsupportedOperationException("not implemented");
	}
}
