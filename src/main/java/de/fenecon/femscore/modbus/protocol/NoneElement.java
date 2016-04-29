package de.fenecon.femscore.modbus.protocol;

import net.wimpi.modbus.procimg.Register;

public class NoneElement extends Element<Object> implements PlaceholderElement {
	public NoneElement(int address, int length, String name) {
		super(address, length, name, "");
	}

	@Override
	public Register[] toRegister(Object value) {
		return null;
	}
}
