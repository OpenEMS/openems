package io.openems.edge.controller.ess.ripplecontrolreceiver;

import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String inputChannelAddress1;
		private String inputChannelAddress2;
		private String inputChannelAddress3;

		private Builder() {
		}

		public Builder setInputChannelAddress1(String inputChannelAddress1) {
			this.inputChannelAddress1 = inputChannelAddress1;
			return this;
		}

		public Builder setInputChannelAddress2(String inputChannelAddress2) {
			this.inputChannelAddress2 = inputChannelAddress2;
			return this;
		}

		public Builder setInputChannelAddress3(String inputChannelAddress3) {
			this.inputChannelAddress3 = inputChannelAddress3;
			return this;
		}

		public Builder setId(String id) {
			this.id = id;
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
	public String inputChannelAddress1() {
		return this.builder.inputChannelAddress1;
	}

	@Override
	public String inputChannelAddress2() {
		return this.builder.inputChannelAddress2;
	}

	@Override
	public String inputChannelAddress3() {
		return this.builder.inputChannelAddress3;
	}
}