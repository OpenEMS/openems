package de.fenecon.femscore.modbus.protocol;

import java.util.List;

import net.wimpi.modbus.procimg.Register;

public class NoneElement extends Element<Object> implements PlaceholderElement {

	public NoneElement(int address, String name, int length) {
		super(address, name, length, "");
	}

	@Override
	protected Object convert(Register register) {
		return null;
	}

	@Override
	protected Object convert(List<Register> registers) {
		return null;
	}

}
