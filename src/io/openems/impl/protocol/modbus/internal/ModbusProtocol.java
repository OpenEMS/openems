package io.openems.impl.protocol.modbus.internal;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.api.channel.Channel;

public class ModbusProtocol {
	private static Logger log = LoggerFactory.getLogger(ModbusProtocol.class);
	private final Map<Channel, Element> channelElementMap;
	private final HashMap<Integer, Range> otherRanges; // key = startAddress
	private final HashMap<Integer, Range> requiredRanges; // key = startAddress

	public ModbusProtocol(Range... ranges) {
		otherRanges = new HashMap<>();
		channelElementMap = new HashMap<>();

		// requiredRanges stays emty till someone calls "setAsRequired()"
		requiredRanges = new HashMap<>();

		for (Range range : ranges) {
			// check each range for plausibility
			checkRange(range);
			// fill otherRanges Map
			otherRanges.put(range.getStartAddress(), range);
			// fill channelElementMap
			for (Element element : range.getElements()) {
				channelElementMap.put(element.getChannel(), element);
			}
		}
	}

	public Collection<Range> getOtherRanges() {
		return otherRanges.values();
	}

	public Collection<Range> getRequiredRanges() {
		return requiredRanges.values();
	}

	public void setAsRequired(Channel channel) {
		Range range = channelElementMap.get(channel).getRange();
		otherRanges.remove(range.getStartAddress());
		requiredRanges.put(range.getStartAddress(), range);
	}

	/**
	 * Checks a {@link Range} for plausibility
	 *
	 * @param range
	 */
	private void checkRange(Range range) {
		int address = range.getStartAddress();
		for (Element element : range.getElements()) {
			if (element.getAddress() != address) {
				log.error("Start address of Element is wrong. It is 0x{}, should be 0x{}",
						Integer.toHexString(element.getAddress()), Integer.toHexString(address));
			}
			address += element.getLength();
			// TODO: check BitElements
		}
	}
}
