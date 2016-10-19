package io.openems.api.channel;

public class ConfigChannelBuilder extends ChannelBuilder<ConfigChannelBuilder> {
	protected Long defaultValue = null;

	@Override
	public ConfigChannel build() {
		return new ConfigChannel(nature, unit, minValue, maxValue, multiplier, delta, labels, defaultValue);
	}

	public ConfigChannelBuilder defaultValue(int defaultValue) {
		this.defaultValue = Long.valueOf(defaultValue);
		return this;
	}

	public ConfigChannelBuilder defaultValue(Long defaultValue) {
		this.defaultValue = defaultValue;
		return this;
	}
}
