package de.fenecon.femscore.modbus.protocol;

import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

import de.fenecon.femscore.modbus.device.counter.CounterProtocol;
import de.fenecon.femscore.modbus.device.ess.EssProtocol;

public class ElementBuilder {
	final int address;
	String name = "";
	ElementType type = ElementType.INTEGER;
	ElementLength length = ElementLength.WORD;
	int intLength = 1;
	int multiplier = 1;
	int delta = 0;
	String unit = "";
	boolean signed = false;
	ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;
	boolean writable = false;
	Map<String, BitElement> bitElements = new HashMap<>();

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

	public ElementBuilder length(ElementLength length) {
		this.length = length;
		if (length == ElementLength.WORD) {
			this.intLength = 1;
		} else if (length == ElementLength.DOUBLEWORD) {
			this.intLength = 2;
		} else {
			this.intLength = 0;
		}
		return this;
	}

	public ElementBuilder intLength(int length) {
		if (intLength == 1) {
			this.length = ElementLength.WORD;
		} else if (intLength == 2) {
			this.length = ElementLength.DOUBLEWORD;
		} else {
			this.length = ElementLength.OTHER;
		}
		this.intLength = length;
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

	public ElementBuilder byteOrder(ByteOrder byteOrder) {
		this.byteOrder = byteOrder;
		return this;
	}

	public ElementBuilder bit(BitElement bitElement) {
		this.bitElements.put(bitElement.getName(), bitElement);
		return this;
	}

	public Element<?> build() {
		Element<?> element = null;
		if (bitElements.size() > 0) {
			element = new BitsElement(address, intLength, name, unit, bitElements);
		} else if (type == ElementType.INTEGER) {
			if (signed) {
				if (length == ElementLength.WORD) {
					element = new SignedIntegerWordElement(address, intLength, name, multiplier, delta, unit,
							byteOrder);
				} else if (length == ElementLength.DOUBLEWORD) {
					element = new SignedIntegerDoublewordElement(address, intLength, name, multiplier, delta, unit,
							byteOrder);
				}
			} else {
				if (length == ElementLength.WORD) {
					element = new UnsignedShortWordElement(address, intLength, name, multiplier, delta, unit);
				} else if (length == ElementLength.DOUBLEWORD) {
					element = new UnsignedIntegerDoublewordElement(address, intLength, name, multiplier, delta, unit);
				}
			}
			// } else if (type == ElementType.DOUBLE) {
			// return new DoubleElement(address, name, multiplier, delta, unit);
		} else if (type == ElementType.TEXT) {
			throw new UnsupportedOperationException("TEXT is not implemented!");
		} else if (type == ElementType.PLACEHOLDER) {
			element = new NoneElement(address, intLength, name);
		}
		if (element != null) {
			return element;
		} else {
			throw new UnsupportedOperationException("ElementBuilder build for " + type + " is not implemented!");
		}
	}
}
