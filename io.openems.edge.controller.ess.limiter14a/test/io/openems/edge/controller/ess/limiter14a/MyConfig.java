package io.openems.edge.controller.ess.limiter14a;

import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String inputChannelAddress;
		private String essId;

		private Builder() {
		}
		
		public Builder setInputChannelAddress(String inputChannelAddress) {
			this.inputChannelAddress = inputChannelAddress;
			return this;
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}
		
		public Builder setEssId(String id) {
			this.essId = id;
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
	public String ess_id() {
		return this.builder.essId;
	}
}