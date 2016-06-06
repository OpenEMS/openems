package de.fenecon.femscore.modbus.protocol;

import java.nio.ByteBuffer;

import com.ghgande.j2mod.modbus.procimg.Register;

import de.fenecon.femscore.modbus.protocol.interfaces.DoublewordElement;

public class UnsignedIntegerDoublewordElement extends NumberElement<Long> implements DoublewordElement {
	protected final boolean inverted;

	public UnsignedIntegerDoublewordElement(int address, int length, String name, int multiplier, int delta,
			String unit, boolean inverted) {
		super(address, length, name, multiplier, delta, unit);
		this.inverted = inverted;
	}

	@Override
	public void update(Register reg1, Register reg2) {
		ByteBuffer buff = ByteBuffer.allocate(4);
		buff.put(reg1.toBytes());
		buff.put(reg2.toBytes());
		long value = Integer.toUnsignedLong(buff.getInt(0) * multiplier - delta);
		if (!inverted) {
			value *= -1;
		}
		update(value);
	}

	@Override
	public Register[] toRegister(Long value) {
		if (!inverted) {
			value *= -1;
		}
		throw new UnsupportedOperationException("not implemented");
	}
}
