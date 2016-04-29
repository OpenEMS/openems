package de.fenecon.femscore.modbus.protocol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModbusProtocol {
	private final static Logger log = LoggerFactory.getLogger(ModbusProtocol.class);

	private final List<ElementRange> elementRanges = new ArrayList<ElementRange>();
	private final Map<String, Element<?>> elements = new HashMap<String, Element<?>>();

	public void addElementRange(ElementRange elementRange) {
		checkElementRange(elementRange);
		elementRanges.add(elementRange);
		for (Element<?> element : elementRange.getElements()) {
			if (!(element instanceof NoneElement)) {
				elements.put(element.getName(), element);
			}
		}
	}

	public Element<?> getElement(String id) {
		return elements.get(id);
	}

	public Set<String> getElementIds() {
		return new HashSet<String>(elements.keySet());
	}

	public List<ElementRange> getElementRanges() {
		return elementRanges;
	}

	/**
	 * Checks an {@link ElementRange} for plausibility
	 * 
	 * @param elementRange
	 *            to be checked
	 */
	private void checkElementRange(ElementRange elementRange) {
		int address = elementRange.getStartAddress();
		for (Element<?> element : elementRange.getElements()) {
			if (element.address != address) {
				log.error("Start address of Element {} is wrong. Should be 0x{}", element.getName(),
						Integer.toHexString(address));
			}
			address += element.getLength();
			// TODO: check BitElements
		}
	}

	@Override
	public String toString() {
		return "ModbusProtocol [elements=" + elements + "]";
	}
}
