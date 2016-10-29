package io.openems.api.channel.numeric;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.openems.api.channel.Channel;
import io.openems.api.channel.ChannelListener;
import io.openems.api.device.nature.DeviceNature;
import io.openems.core.databus.Databus;

public abstract class AggregatedNumericChannel extends NumericChannel implements ChannelListener {
	private final Map<NumericChannel, Optional<Long>> values = new HashMap<>();

	public AggregatedNumericChannel(Optional<String> channelId, DeviceNature nature, String unit, Long minValue,
			Long maxValue, Long multiplier, Long delta, Map<Long, String> labels, Set<NumericChannel> channels) {
		super(channelId, nature, unit, minValue, maxValue, multiplier, delta, labels);
		for (NumericChannel channel : channels) {
			values.put(channel, Optional.empty());
		}
	}

	@Override
	public void setDatabus(Databus databus) {
		super.setDatabus(databus);
		for (NumericChannel channel : values.keySet()) {
			databus.addListener(channel, this);
		}
	}

	@Override
	public Optional<Long> getValueOptional() {
		return Optional.of(aggregateValue(this.values));
	}

	@Override
	public void channelUpdated(Channel<?> channel) {
		if (values.containsKey(channel)) {
			if (channel instanceof NumericChannel) {
				NumericChannel nChannel = (NumericChannel) channel;
				values.put(nChannel, nChannel.getValueOptional());
			} else {
				log.warn("Channel [" + channel.getAddress() + "] is not a NumericChannel.");
			}
		} else {
			log.warn("Channel [" + channel.getAddress() + "] does not belong to [" + this.getAddress() + "].");
		}
		Long newValue = aggregateValue(this.values);
		super.updateValue(newValue);
	}

	protected abstract Long aggregateValue(Map<NumericChannel, Optional<Long>> values);
}
