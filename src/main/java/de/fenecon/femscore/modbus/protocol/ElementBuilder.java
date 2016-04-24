package de.fenecon.femscore.modbus.protocol;

import de.fenecon.femscore.modbus.device.ess.EssProtocol;

public class ElementBuilder {
	String name = "";
	ElementType type = ElementType.INTEGER;
	int length = 1;
	int multiplier = 1;
	int delta = 0;
	String unit = "";
	boolean signed = false;
	boolean littleEndian = false;

	public ElementBuilder() {

	}

	public ElementBuilder(int address) {
		// ignore
	}

	public ElementBuilder name(String name) {
		this.name = name;
		return this;
	}

	public ElementBuilder name(EssProtocol name) {
		this.name = name.name();
		return this;
	}

	public ElementBuilder length(int words) {
		this.length = words;
		return this;
	}

	public ElementBuilder type(ElementType type) {
		this.type = type;
		return this;
	}

	public ElementBuilder multiplier(int multiplier) {
		this.multiplier = multiplier;
		return this;
	}

	public ElementBuilder delta(int delta) {
		this.delta = delta;
		return this;
	}

	public ElementBuilder unit(String unit) {
		this.unit = unit;
		return this;
	}

	public ElementBuilder signed(boolean signed) {
		this.signed = signed;
		return this;
	}

	public ElementBuilder littleEndian(boolean littleEndian) {
		this.littleEndian = littleEndian;
		return this;
	}

	public Element<?> build() {
		if (type == ElementType.INTEGER) {
			if (signed) {
				return new SignedIntegerElement(name, length, multiplier, delta, unit, littleEndian);
			} else {
				return new UnsignedIntegerElement(name, length, multiplier, delta, unit);
			}
		} else if (type == ElementType.DOUBLE) {
			return new DoubleElement(name, length, multiplier, delta, unit);
		} else if (type == ElementType.TEXT) {
			throw new UnsupportedOperationException("TEXT is not implemented!");
		}
		throw new UnsupportedOperationException("ElementBuilder build is not implemented!");
	}
}
