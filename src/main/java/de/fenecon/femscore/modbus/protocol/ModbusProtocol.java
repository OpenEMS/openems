package de.fenecon.femscore.modbus.protocol;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fenecon.femscore.modbus.ModbusConnection;
import net.wimpi.modbus.procimg.Register;

public class ModbusProtocol {
	private final static Logger log = LoggerFactory.getLogger(ModbusProtocol.class);

	private final List<ElementRange> elementRanges = new ArrayList<ElementRange>();
	private final Map<String, Element<?>> elements = new HashMap<String, Element<?>>();

	public void addElementRange(ElementRange elementRange) {
		checkElementRange(elementRange);
		elementRanges.add(elementRange);
		for (Element<?> element : elementRange.getElements()) {
			if (!(element instanceof PlaceholderElement)) {
				elements.put(element.getName(), element);
			}
		}
	}

	public void query(ModbusConnection modbusConnection, int unitid) throws Exception {
		for (ElementRange elementRange : elementRanges) {
			Register[] registers = modbusConnection.query(unitid, elementRange.getStartAddress(),
					elementRange.getTotalLength());
			int position = 0;
			for (Element<?> element : elementRange.getElements()) {
				int length = element.getLength();
				int nextPosition = position + length;
				if (length == 1) {
					element.update(registers[position]);
				} else {
					element.update(Arrays.asList(registers).subList(position, nextPosition));
				}
				position = nextPosition;
			}
		}
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
		}
	}

	@Override
	public String toString() {
		return "ModbusProtocol [elements=" + elements + "]";
	}
}
