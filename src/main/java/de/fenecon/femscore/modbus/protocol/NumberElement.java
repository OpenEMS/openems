package de.fenecon.femscore.modbus.protocol;

public abstract class NumberElement<T> extends Element<T> {
	final int multiplier;
	final String unit;
	final int delta;

	public NumberElement(String name, int length, int multiplier, int delta, String unit) {
		super(name, length);
		this.multiplier = multiplier;
		this.unit = unit;
		this.delta = delta;
	}
}
