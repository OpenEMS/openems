package de.fenecon.femscore.modbus.protocol;

import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ghgande.j2mod.modbus.procimg.Register;

import de.fenecon.femscore.modbus.protocol.interfaces.WordElement;

public class UnsignedShortWordElement extends NumberElement<Integer> implements WordElement {
	@SuppressWarnings("unused")
	private final static Logger log = LoggerFactory.getLogger(UnsignedShortWordElement.class);

	public UnsignedShortWordElement(int address, int length, String name, int multiplier, int delta, String unit) {
		super(address, length, name, multiplier, delta, unit);
	}

	@Override
	public void update(Register register) {
		ByteBuffer buff = ByteBuffer.allocate(2);
		buff.put(register.toBytes());
		update(Short.toUnsignedInt((short) (buff.getShort(0) * multiplier - delta)));
	}

	@Override
	public Register[] toRegister(Integer value) {
		throw new UnsupportedOperationException("not implemented");
	}
}
