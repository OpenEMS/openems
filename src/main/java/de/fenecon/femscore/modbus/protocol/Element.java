package de.fenecon.femscore.modbus.protocol;

import net.wimpi.modbus.procimg.Register;

public abstract class Element<T> {
	protected final String name;
	protected final int length;

	public Element(String name, int length) {
		this.name = name;
		this.length = length;
	}

	public int getLength() {
		return length;
	}

	public T convert(Register[] registers) {
		if (length == 1) {
			return _convert(registers[0]);
		} else {
			return _convert(registers);
		}
	};

	protected abstract T _convert(Register register);

	protected abstract T _convert(Register[] registers);
}
