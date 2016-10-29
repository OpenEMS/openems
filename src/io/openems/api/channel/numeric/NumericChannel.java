package io.openems.api.channel.numeric;

import java.util.Map;
import java.util.Optional;

import io.openems.api.channel.Channel;
import io.openems.api.device.nature.DeviceNature;
import io.openems.api.exception.InvalidValueException;

public class NumericChannel extends Channel<Long> {
	protected final Long delta;
	protected final Optional<Map<Long, String>> labels;
	protected final Long multiplier;
	private Optional<Long> maxValue = Optional.empty();
	private Optional<Long> minValue = Optional.empty();
	private final String unit;

	public NumericChannel(Optional<String> channelId, DeviceNature nature, String unit, Long minValue, Long maxValue,
			Long multiplier, Long delta, Map<Long, String> labels) {
		super(nature, channelId);
		this.unit = unit;
		this.multiplier = multiplier;
		this.delta = delta;
		this.minValue = Optional.ofNullable(minValue);
		this.maxValue = Optional.ofNullable(maxValue);
		this.labels = Optional.ofNullable(labels);
	}

	public Long getMaxValue() throws InvalidValueException {
		return maxValue.orElseThrow(() -> new InvalidValueException("No Max-Value available."));
	}

	public Optional<Long> getMaxValueOptional() {
		return maxValue;
	}

	public Long getMinValue() throws InvalidValueException {
		return minValue.orElseThrow(() -> new InvalidValueException("No Min-Value available."));
	}

	public Optional<Long> getMinValueOptional() {
		return minValue;
	}

	public String getUnit() {
		return unit;
	}

	// TODO: return a list of Strings
	public Optional<String> getValueLabelOptional() {
		String label;
		Optional<Long> value = getValueOptional();
		if (value.isPresent() && labels.isPresent() && labels.get().containsKey(value.get())) {
			label = labels.get().get(value.get());
			return Optional.of(label);
		}
		return Optional.empty();
	};

	public String format() {
		Optional<String> label = getValueLabelOptional();
		Optional<Long> value = getValueOptional();
		if (label.isPresent()) {
			return label.get();
		} else if (value.isPresent()) {
			return value.get() + " " + unit;
		} else {
			return "INVALID";
		}
	}

	@Override
	protected void updateValue(Long value, boolean triggerDatabusEvent) {
		if (value != null) {
			value = value * multiplier - delta;
		}
		super.updateValue(value, triggerDatabusEvent);
	}

}
