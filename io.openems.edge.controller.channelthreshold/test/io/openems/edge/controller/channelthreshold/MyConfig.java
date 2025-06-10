package io.openems.edge.controller.channelthreshold;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.types.ChannelAddress;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String inputChannelAddress;
		private String outputChannelAddress;
		private int lowThreshold;
		private int highThreshold;
		private int hysteresis;
		private boolean invert;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setInputChannelAddress(ChannelAddress inputChannelAddress) {
			this.inputChannelAddress = inputChannelAddress.toString();
			return this;
		}

		public Builder setOutputChannelAddress(ChannelAddress outputChannelAddress) {
			this.outputChannelAddress = outputChannelAddress.toString();
			return this;
		}

		public Builder setLowThreshold(int lowThreshold) {
			this.lowThreshold = lowThreshold;
			return this;
		}

		public Builder setHighThreshold(int highThreshold) {
			this.highThreshold = highThreshold;
			return this;
		}

		public Builder setHysteresis(int hysteresis) {
			this.hysteresis = hysteresis;
			return this;
		}

		public Builder setInvert(boolean invert) {
			this.invert = invert;
			return this;
		}

		public MyConfig build() {
			return new MyConfig(this);
		}
	}

	/**
	 * Create a Config builder.
	 *
	 * @return a {@link Builder}
	 */
	public static Builder create() {
		return new Builder();
	}

	private final Builder builder;

	private MyConfig(Builder builder) {
		super(Config.class, builder.id);
		this.builder = builder;
	}

	@Override
	public String inputChannelAddress() {
		return this.builder.inputChannelAddress;
	}

	@Override
	public String outputChannelAddress() {
		return this.builder.outputChannelAddress;
	}

	@Override
	public int lowThreshold() {
		return this.builder.lowThreshold;
	}

	@Override
	public int highThreshold() {
		return this.builder.highThreshold;
	}

	@Override
	public int hysteresis() {
		return this.builder.hysteresis;
	}

	@Override
	public boolean invert() {
		return this.builder.invert;
	}

}
