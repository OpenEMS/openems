package de.fenecon.femscore.modbus.protocol;

import de.fenecon.femscore.modbus.protocol.interfaces.WordElement;

/**
 * This represents an Element that is only one bit long.
 * 
 * @author stefan.feilmeier
 */
public abstract class BitElement<T> extends Element<T> implements WordElement {
	public BitElement(int address, String name, String unit) {
		super(address, 1, name, unit);
	}
}
