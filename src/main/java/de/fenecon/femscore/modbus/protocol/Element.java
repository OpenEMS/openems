package de.fenecon.femscore.modbus.protocol;

import java.util.HashSet;
import java.util.Set;

import org.joda.time.DateTime;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ghgande.j2mod.modbus.procimg.Register;

import de.fenecon.femscore.modbus.protocol.interfaces.ElementUpdateListener;

public abstract class Element<T> {
	@SuppressWarnings("unused")
	private final static Logger log = LoggerFactory.getLogger(Element.class);

	protected final int address;
	protected final int length;
	protected final String name;
	protected final String unit;
	protected final Period validPeriod;
	protected Set<ElementUpdateListener> listeners = new HashSet<>();

	protected DateTime lastUpdate = null;
	protected T value = null;
	protected ElementRange elementRange = null;

	public Element(int address, int length, String name, String unit) {
		this.address = address;
		this.length = length;
		this.name = name;
		this.unit = unit;
		this.validPeriod = new Period(Period.minutes(1));
	}

	public int getAddress() {
		return address;
	}

	public int getLength() {
		return length;
	}

	public String getName() {
		return name;
	}

	public String getUnit() {
		return unit;
	}

	public T getValue() {
		// TODO: check if valid is still valid
		return value;
	}

	public void addListener(ElementUpdateListener listener) {
		listeners.add(listener);
	}

	private void notifyListeners() {
		for (ElementUpdateListener listener : listeners) {
			listener.elementUpdated(this.name, this.value);
		}
	}

	/**
	 * Returns the raw value, without checking if it is still valid
	 * 
	 * @return unchecked, raw value
	 */
	public T getRawValue() {
		return value;
	}

	public void setElementRange(ElementRange elementRange) {
		this.elementRange = elementRange;
	}

	public ElementRange getElementRange() {
		return elementRange;
	}

	/**
	 * Gets the timestamp of the last update, null if no update ever happened
	 * 
	 * @return last update timestamp
	 */
	public DateTime getLastUpdate() {
		return lastUpdate;
	}

	public abstract Register[] toRegister(T value);

	/**
	 * Updates the lastUpdate timestamp. Always call this method with any
	 * "update" method
	 * 
	 */
	protected void update(T value) {
		lastUpdate = DateTime.now();
		this.value = value;
		notifyListeners();
	};

	@Override
	public String toString() {
		return "Element [address=0x" + Integer.toHexString(address) + ", name=" + name + ", unit=" + unit
				+ ", lastUpdate=" + lastUpdate + ", value=" + value + "]";
	}

	public String readable() {
		return String.format("%5d %s", value, unit);
	}
}
