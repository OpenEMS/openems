package de.fenecon.femscore.modbus.protocol;

public abstract class NumberElement<T> extends Element<T> {
	protected final int multiplier;
	protected final int delta;

	public NumberElement(int address, int length, String name, int multiplier, int delta, String unit) {
		super(address, length, name, unit);
		this.multiplier = multiplier;
		this.delta = delta;
	}
}
