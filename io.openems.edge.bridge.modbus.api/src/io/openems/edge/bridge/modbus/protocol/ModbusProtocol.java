package io.openems.edge.bridge.modbus.protocol;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModbusProtocol {

	private final Logger log = LoggerFactory.getLogger(ModbusProtocol.class);

	/**
	 * All ranges. Key is startAddress
	 */
	private final Map<Integer, Range> ranges = new HashMap<>();

	/**
	 * All WriteRanges. Key is startAddress
	 */
	private final Map<Integer, WriteRange> writeRanges = new HashMap<>();

	public ModbusProtocol(Range... ranges) {
		for (Range range : ranges) {
			addRange(range);
		}
	}

	public void addRange(Range range) {
		// check each range for plausibility
		this.checkRange(range);
		// fill writeRanges
		if (range instanceof WriteRange) {
			WriteRange writableRange = (WriteRange) range;
			this.writeRanges.put(writableRange.getStartAddress(), writableRange);
		}
		// fill readRanges Map
		this.ranges.put(range.getStartAddress(), range);
	}

	public Collection<Range> getRanges() {
		return Collections.unmodifiableCollection(this.ranges.values());
	}

	public Collection<WriteRange> getWriteRanges() {
		return Collections.unmodifiableCollection(this.writeRanges.values());
	}

	/**
	 * Checks a {@link Range} for plausibility
	 *
	 * @param range
	 */
	private void checkRange(Range range) {
		int address = range.getStartAddress();
		for (RegisterElement<?> element : range.getElements()) {
			if (element.getStartAddress() != address) {
				log.error("Start address is wrong. It is [" + element.getStartAddress() + "/0x"
						+ Integer.toHexString(element.getStartAddress()) + "] but should be [" + address + "/0x"
						+ Integer.toHexString(address) + "].");
			}
			address += element.getLength();
			// TODO: check BitElements
		}
	}
}
