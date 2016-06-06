package de.fenecon.femscore.modbus.protocol;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.ghgande.j2mod.modbus.procimg.Register;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;

import de.fenecon.femscore.modbus.protocol.interfaces.WordElement;

public class SignedIntegerWordElement extends NumberElement<Integer> implements WordElement {
	final ByteOrder byteOrder;
	protected final boolean inverted;

	public SignedIntegerWordElement(int address, int length, String name, int multiplier, int delta, String unit,
			ByteOrder byteOrder, boolean inverted) {
		super(address, length, name, multiplier, delta, unit);
		this.byteOrder = byteOrder;
		this.inverted = inverted;
	}

	@Override
	public void update(Register register) {
		ByteBuffer buff = ByteBuffer.allocate(2).order(byteOrder);
		buff.put(register.toBytes());
		int value = buff.order(byteOrder).getShort(0) * multiplier - delta;
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
		byte[] b = ByteBuffer.allocate(2).order(byteOrder)
				.putShort(new Integer((value - delta) / multiplier).shortValue()).array();
		return new Register[] { new SimpleRegister(b[0], b[1]) };
	}
}
