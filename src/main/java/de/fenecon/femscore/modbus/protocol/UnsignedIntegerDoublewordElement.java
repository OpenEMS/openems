package de.fenecon.femscore.modbus.protocol;

import java.nio.ByteBuffer;

import com.ghgande.j2mod.modbus.procimg.Register;

import de.fenecon.femscore.modbus.protocol.interfaces.DoublewordElement;

public class UnsignedIntegerDoublewordElement extends NumberElement<Long> implements DoublewordElement {
	public UnsignedIntegerDoublewordElement(int address, int length, String name, int multiplier, int delta,
			String unit) {
		super(address, length, name, multiplier, delta, unit);
	}

	@Override
	public void update(Register reg1, Register reg2) {
		ByteBuffer buff = ByteBuffer.allocate(4);
		buff.put(reg1.toBytes());
		buff.put(reg2.toBytes());
		update(Integer.toUnsignedLong(buff.getInt(0) * multiplier - delta));
	}

	@Override
	public Register[] toRegister(Long value) {
		throw new UnsupportedOperationException("not implemented");
	}
}
