package io.openems.edge.controller.io.channelsinglethreshold;

import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private Mode mode;
		private String inputChannelAddress;
		private int threshold;
		private int switchedLoadPower;
		private int minimumSwitchingTime;
		private boolean invert;
		private String[] outputChannelAddress;

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

		public Builder setInvert(boolean invert) {
			this.invert = invert;
			return this;
		}

		public Builder setMinimumSwitchingTime(int minimumSwitchingTime) {
			this.minimumSwitchingTime = minimumSwitchingTime;
			return this;
		}

		public Builder setSwitchedLoadPower(int switchedLoadPower) {
			this.switchedLoadPower = switchedLoadPower;
			return this;
		}

		public Builder setThreshold(int threshold) {
			this.threshold = threshold;
			return this;
		}

		public Builder setOutputChannelAddress(String... outputChannelAddress) {
			this.outputChannelAddress = outputChannelAddress;
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
	public int threshold() {
		return this.builder.threshold;
	}

	@Override
	public int switchedLoadPower() {
		return this.builder.switchedLoadPower;
	}

	@Override
	public int minimumSwitchingTime() {
		return this.builder.minimumSwitchingTime;
	}

	@Override
	public boolean invert() {
		return this.builder.invert;
	}

	@Override
	public String[] outputChannelAddress() {
		return this.builder.outputChannelAddress;
	}

}