package io.openems.api.channel;

import java.math.BigInteger;

public class ConfigChannelBuilder extends ChannelBuilder<ConfigChannelBuilder> {
	protected BigInteger defaultValue = null;

	@Override
	public ConfigChannel build() {
		return new ConfigChannel(nature, unit, minValue, maxValue, multiplier, delta, labels, defaultValue);
	}

	public ConfigChannelBuilder defaultValue(BigInteger defaultValue) {
		this.defaultValue = defaultValue;
		return this;
	}

	public ConfigChannelBuilder defaultValue(int defaultValue) {
		this.defaultValue = BigInteger.valueOf(defaultValue);
		return this;
	}
}
