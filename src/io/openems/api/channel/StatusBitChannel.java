package io.openems.api.channel;

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;

import io.openems.api.device.nature.DeviceNature;
import io.openems.impl.protocol.modbus.ModbusReadChannel;

public class StatusBitChannel extends ModbusReadChannel {

	public StatusBitChannel(String id, DeviceNature nature) {
		super(id, nature);
	}

	/**
	 * Get labels for all set bits
	 * Example: Value is 5, Labels are 1=One; 2=Two; 4=Four -> this method returns [One, Four]
	 *
	 * @return
	 */
	public Set<String> labels() {
		Set<String> result = new HashSet<>();
		Optional<Long> valueOptional = valueOptional();
		if (valueOptional.isPresent()) {
			long value = valueOptional.get();
			for (Entry<Long, String> entry : this.labels.descendingMap().entrySet()) {
				if (entry.getKey() <= value) {
					result.add(entry.getValue());
					value -= entry.getKey();
				}
			}
		}
		return result;
	};

	@Override public Optional<String> labelOptional() {
		Set<String> labels = this.labels();
		if (labels.isEmpty()) {
			return Optional.empty();
		} else {
			StringJoiner joiner = new StringJoiner(",");
			for (String label : labels) {
				joiner.add(label);
			}
			return Optional.of(joiner.toString());
		}
	};

	@Override public StatusBitChannel label(Long value, String label) {
		return (StatusBitChannel) super.label(value, label);
	}

	@Override public StatusBitChannel label(int value, String label) {
		return (StatusBitChannel) super.label(Long.valueOf(value), label);
	}
}
