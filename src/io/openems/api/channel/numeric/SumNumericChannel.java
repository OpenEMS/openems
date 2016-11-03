package io.openems.api.channel.numeric;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import io.openems.api.device.nature.DeviceNature;

public class SumNumericChannel extends AggregatedNumericChannel {

	public SumNumericChannel(Optional<String> channelId, DeviceNature nature, String unit, Long minValue, Long maxValue,
			Long multiplier, Long delta, Map<Long, String> labels, Set<NumericChannel> channels) {
		super(channelId, nature, unit, minValue, maxValue, multiplier, delta, labels, channels);
	}

	@Override
	protected Long aggregateValue(Map<NumericChannel, Optional<Long>> values) {
		Long result = 0L;
		for (Entry<NumericChannel, Optional<Long>> entry : values.entrySet()) {
			// log.info(entry.getValue().orElse(0L).toString());
			result += entry.getValue().orElse(0L);
		}
		return result;
	}
}
