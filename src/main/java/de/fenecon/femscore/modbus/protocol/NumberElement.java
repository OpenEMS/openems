package de.fenecon.femscore.modbus.protocol;

public abstract class NumberElement<T> extends Element<T> {
	protected final int multiplier;
	protected final int delta;

	public NumberElement(int address, String name, int length, int multiplier, int delta, String unit) {
		super(address, name, length, unit);
		this.multiplier = multiplier;
		this.delta = delta;
	}
}
