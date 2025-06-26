package io.openems.edge.controller.io.fixdigitaloutput;

import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private boolean isOn;
		private String outputChannelAddress;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setOn(boolean isOn) {
			this.isOn = isOn;
			return this;
		}

		public Builder setOutputChannelAddress(String outputChannelAddress) {
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
	public String outputChannelAddress() {
		return this.builder.outputChannelAddress;
	}

	@Override
	public boolean isOn() {
		return this.builder.isOn;
	}
}