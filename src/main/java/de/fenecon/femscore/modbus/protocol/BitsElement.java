package de.fenecon.femscore.modbus.protocol;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ghgande.j2mod.modbus.procimg.Register;

import de.fenecon.femscore.modbus.protocol.interfaces.DoublewordElement;
import de.fenecon.femscore.modbus.protocol.interfaces.WordElement;

public class BitsElement extends Element<Map<String, BitElement<?>>> implements WordElement, DoublewordElement {
	@SuppressWarnings("unused")
	private final static Logger log = LoggerFactory.getLogger(BitsElement.class);

	protected final Map<String, BitElement<?>> bitElements;

	public BitsElement(int address, int length, String name, String unit, Map<String, BitElement<?>> bitElements) {
		super(address, length, name, unit);
		this.bitElements = bitElements;
	}

	public BitElement<?> getBit(String id) {
		return bitElements.get(id);
	}

	@Override
	public void update(Register register) {
		for (BitElement<?> bitElement : bitElements.values()) {
			bitElement.update(register);
		}
		// TODO update();
	}

	@Override
	public void update(Register reg1, Register reg2) {
		update(reg1);
		update(reg2);
		// TODO update();
	}

	@Override
	public Register[] toRegister(Map<String, BitElement<?>> value) {
		throw new UnsupportedOperationException("not implemented");
	}
}
