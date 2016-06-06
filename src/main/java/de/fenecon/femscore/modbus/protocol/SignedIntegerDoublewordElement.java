package de.fenecon.femscore.modbus.protocol;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.ghgande.j2mod.modbus.procimg.Register;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;

import de.fenecon.femscore.modbus.protocol.interfaces.DoublewordElement;

public class SignedIntegerDoublewordElement extends NumberElement<Integer> implements DoublewordElement {
	private final ByteOrder byteOrder;
	protected final boolean inverted;

	public SignedIntegerDoublewordElement(int address, int length, String name, int multiplier, int delta, String unit,
			ByteOrder byteOrder, boolean inverted) {
		super(address, length, name, multiplier, delta, unit);
		this.byteOrder = byteOrder;
		this.inverted = inverted;
	}

	@Override
	public void update(Register reg1, Register reg2) {
		ByteBuffer buff = ByteBuffer.allocate(4).order(byteOrder);
		buff.put(reg1.toBytes());
		buff.put(reg2.toBytes());
		int value = buff.order(byteOrder).getInt(0) * multiplier - delta;
		if (inverted) {
			value *= -1;
		}
		update(value);
	}

	@Override
	public Register[] toRegister(Integer value) {
		if (inverted) {
			value *= -1;
		}
		byte[] b = ByteBuffer.allocate(4).order(byteOrder).putInt((value - delta) / multiplier).array();
		return new Register[] { new SimpleRegister(b[0], b[1]), new SimpleRegister(b[2], b[3]) };
	}
}
