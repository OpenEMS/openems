package de.fenecon.femscore.modbus.protocol;

import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ghgande.j2mod.modbus.procimg.Register;

import de.fenecon.femscore.modbus.protocol.interfaces.WordElement;

public class UnsignedShortWordElement extends NumberElement<Integer> implements WordElement {
	@SuppressWarnings("unused")
	private final static Logger log = LoggerFactory.getLogger(UnsignedShortWordElement.class);
	protected final boolean inverted;

	public UnsignedShortWordElement(int address, int length, String name, int multiplier, int delta, String unit,
			boolean inverted) {
		super(address, length, name, multiplier, delta, unit);
		this.inverted = inverted;
	}

	@Override
	public void update(Register register) {
		ByteBuffer buff = ByteBuffer.allocate(2);
		buff.put(register.toBytes());
		int value = Short.toUnsignedInt((short) (buff.getShort(0) * multiplier - delta));
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
		throw new UnsupportedOperationException("not implemented");
	}
}
