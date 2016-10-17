package io.openems.impl.protocol.modbus.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.api.channel.Channel;
import io.openems.impl.protocol.modbus.ModbusElement;

public class ModbusProtocol {
	private static Logger log = LoggerFactory.getLogger(ModbusProtocol.class);
	private final Map<Channel, ModbusElement> channelElementMap = new HashMap<>();
	private final HashMap<Integer, ModbusRange> otherRanges = new HashMap<>(); // key = startAddress
	// requiredRanges stays empty till someone calls "setAsRequired()"
	private final HashMap<Integer, ModbusRange> requiredRanges = new HashMap<>(); // key = startAddress

	public ModbusProtocol(ModbusRange... ranges) {
		for (ModbusRange range : ranges) {
			// check each range for plausibility
			checkRange(range);
			// fill otherRanges Map
			otherRanges.put(range.getStartAddress(), range);
			// fill channelElementMap
			for (ModbusElement element : range.getElements()) {
				channelElementMap.put(element.getChannel(), element);
			}
		}
	}

	public synchronized Collection<ModbusRange> getOtherRanges() {
		if (otherRanges.isEmpty()) {
			return new ArrayList<ModbusRange>();
		}
		return otherRanges.values();
	}

	public synchronized Collection<ModbusRange> getRequiredRanges() {
		if (requiredRanges.isEmpty()) {
			return new ArrayList<ModbusRange>();
		}
		return requiredRanges.values();
	}

	public synchronized void setAsRequired(Channel channel) {
		log.debug("Set required: " + channel + " " + this);
		ModbusRange range = channelElementMap.get(channel).getModbusRange();
		otherRanges.remove(range.getStartAddress());
		requiredRanges.put(range.getStartAddress(), range);
	}

	/**
	 * Checks a {@link ModbusRange} for plausibility
	 *
	 * @param range
	 */
	private void checkRange(ModbusRange range) {
		int address = range.getStartAddress();
		for (ModbusElement element : range.getElements()) {
			if (element.getAddress() != address) {
				log.error("Start address of Element is wrong. It is 0x{}, should be 0x{}",
						Integer.toHexString(element.getAddress()), Integer.toHexString(address));
			}
			address += element.getLength();
			// TODO: check BitElements
		}
	}
}
