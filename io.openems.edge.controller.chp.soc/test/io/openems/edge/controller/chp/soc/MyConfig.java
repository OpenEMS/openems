package io.openems.edge.controller.chp.soc;

import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id = null;
		private Mode mode;
		private String inputChannelAddress;
		private String outputChannelAddress;
		private int lowThreshold;
		private int highThreshold;
		private boolean invert;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setMode(Mode mode) {
			this.mode = mode;
			return this;
		}

		public Builder setInputChannelAddress(String inputChannelAddress) {
			this.inputChannelAddress = inputChannelAddress;
			return this;
		}

		public Builder setOutputChannelAddress(String outputChannelAddress) {
			this.outputChannelAddress = outputChannelAddress;
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
	public Mode mode() {
		return this.builder.mode;
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
	public boolean invert() {
		return this.builder.invert;
	}

}
