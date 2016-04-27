package de.fenecon.femscore.modbus.protocol;

import java.util.HashMap;
import java.util.Map;

import de.fenecon.femscore.modbus.device.counter.CounterProtocol;
import de.fenecon.femscore.modbus.device.ess.EssProtocol;

public class ElementBuilder {
	final int address;
	String name = "";
	ElementType type = ElementType.INTEGER;
	int length = 1;
	int multiplier = 1;
	int delta = 0;
	String unit = "";
	boolean signed = false;
	boolean littleEndian = false;
	Map<String, BitElement<?>> bitElements = new HashMap<String, BitElement<?>>();

	public ElementBuilder(int address) {
		this.address = address;
	}

	public ElementBuilder name(String name) {
		this.name = name;
		return this;
	}

	public ElementBuilder name(EssProtocol name) {
		this.name = name.name();
		return this;
	}

	public ElementBuilder name(CounterProtocol name) {
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

	public ElementBuilder bit(BitElement<?> bitElement) {
		this.bitElements.put(bitElement.getName(), bitElement);
		return this;
	}

	public Element<?> build() {
		if (bitElements.size() > 0) {
			return new BitsElement(address, name, length, unit, bitElements);
		} else if (type == ElementType.INTEGER) {
			if (signed) {
				return new SignedIntegerElement(address, name, length, multiplier, delta, unit, littleEndian);
			} else {
				return new UnsignedIntegerElement(address, name, length, multiplier, delta, unit);
			}
		} else if (type == ElementType.DOUBLE) {
			return new DoubleElement(address, name, length, multiplier, delta, unit);
		} else if (type == ElementType.TEXT) {
			throw new UnsupportedOperationException("TEXT is not implemented!");
		} else if (type == ElementType.PLACEHOLDER) {
			return new NoneElement(address, name, length);
		}
		throw new UnsupportedOperationException("ElementBuilder build for " + type + " is not implemented!");
	}
}
