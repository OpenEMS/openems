package de.fenecon.femscore.modbus.protocol;

import com.ghgande.j2mod.modbus.procimg.Register;

public class NoneBitElement extends BitElement<Object> implements PlaceholderElement {
	public NoneBitElement(int address, int length, String name) {
		super(address, name, "");
	}

	@Override
	public Register[] toRegister(Object value) {
		return null;
	}

	@Override
	public void update(Register register) {
		// nothing to do
	}
}
