package de.fenecon.femscore.modbus.protocol;

import java.util.List;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.wimpi.modbus.procimg.Register;

public abstract class Element<T> {
	@SuppressWarnings("unused")
	private final static Logger log = LoggerFactory.getLogger(Element.class);

	protected final int address;
	protected final String name;
	protected final int length;
	protected final String unit;
	protected DateTime lastUpdate = null;
	protected T value = null;

	public Element(int address, String name, int length, String unit) {
		this.address = address;
		this.name = name;
		this.length = length;
		this.unit = unit;
	}

	public int getAddress() {
		return address;
	}

	public String getName() {
		return name;
	}

	public int getLength() {
		return length;
	}

	public String getUnit() {
		return unit;
	}

	public T getValue() {
		return value;
	}

	/**
	 * Gets the timestamp of the last update, null if no update ever happened
	 * 
	 * @return last update timestamp
	 */
	public DateTime getLastUpdate() {
		return lastUpdate;
	}

	public void update(List<Register> registers) {
		value = convert(registers);
		lastUpdate = DateTime.now();
	};

	public void update(Register register) {
		value = convert(register);
		lastUpdate = DateTime.now();
	};

	protected abstract T convert(Register register);

	protected abstract T convert(List<Register> registers);

	@Override
	public String toString() {
		return "Element [address=0x" + Integer.toHexString(address) + ", name=" + name + ", length=" + length
				+ ", unit=" + unit + ", lastUpdate=" + lastUpdate + ", value=" + value + "]";
	}
}
