package de.fenecon.femscore.modbus.protocol;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ghgande.j2mod.modbus.procimg.Register;

public class BooleanBitElement extends BitElement<Boolean> {
	@SuppressWarnings("unused")
	private final static Logger log = LoggerFactory.getLogger(BitElement.class);

	public BooleanBitElement(int address, String name) {
		super(address, name, "");
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Boolean getValue() {
		return value;
	}

	/**
	 * Gets the timestamp of the last update, null if no update ever happened
	 * 
	 * @return last update timestamp
	 */
	@Override
	public DateTime getLastUpdate() {
		return lastUpdate;
	}

	@Override
	public String toString() {
		return "Element [address=0x" + Integer.toHexString(address) + ", name=" + name + ", unit=" + unit
				+ ", lastUpdate=" + lastUpdate + ", value=" + value + "]";
	}

	@Override
	public void update(Register register) {
		int position = address % 8;
		byte curByte = register.toBytes()[1 - address / 8];
		update(new Boolean(((curByte >> position) & 1) == 1));
	}

	@Override
	public Register[] toRegister(Boolean value) {
		throw new UnsupportedOperationException("not implemented");
	}
}
