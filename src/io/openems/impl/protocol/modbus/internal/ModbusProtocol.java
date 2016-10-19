package io.openems.impl.protocol.modbus.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.api.channel.Channel;
import io.openems.impl.protocol.modbus.ModbusElement;

public class ModbusProtocol {
	private static Logger log = LoggerFactory.getLogger(ModbusProtocol.class);
	private final Map<Channel, ModbusElement> channelElementMap = new ConcurrentHashMap<>();
	private final Map<Integer, ModbusRange> otherRanges = new ConcurrentHashMap<>(); // key = startAddress
	// requiredRanges stays empty till someone calls "setAsRequired()"
	private final Map<Integer, ModbusRange> requiredRanges = new ConcurrentHashMap<>(); // key = startAddress
	private final Map<Integer, WritableModbusRange> writableRanges = new ConcurrentHashMap<>(); // key = startAddress

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
			// fill writableRanges
			if (range instanceof WritableModbusRange) {
				WritableModbusRange writableRange = (WritableModbusRange) range;
				writableRanges.put(writableRange.getStartAddress(), writableRange);
			}
		}
	}

	public synchronized Collection<ModbusRange> getOtherRanges() {
		if (otherRanges.isEmpty()) {
			return Collections.unmodifiableCollection(new ArrayList<ModbusRange>());
		}
		return Collections.unmodifiableCollection(otherRanges.values());
	}

	public synchronized Collection<ModbusRange> getRequiredRanges() {
		if (requiredRanges.isEmpty()) {
			return Collections.unmodifiableCollection(new ArrayList<ModbusRange>());
		}
		return Collections.unmodifiableCollection(requiredRanges.values());
	}

	public synchronized Collection<WritableModbusRange> getWritableRanges() {
		if (writableRanges.isEmpty()) {
			return Collections.unmodifiableCollection(new ArrayList<WritableModbusRange>());
		}
		return Collections.unmodifiableCollection(writableRanges.values());
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
