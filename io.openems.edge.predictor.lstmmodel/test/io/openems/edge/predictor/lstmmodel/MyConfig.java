package io.openems.edge.predictor.lstmmodel;

import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		public String[] channelAddresses;
		public int numOfWeeks;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setChannelAddresses(String... channelAddresses) {
			this.channelAddresses = channelAddresses;
			return this;
		}

		public Builder setNumOfWeeks(int numOfWeeks) {
			this.numOfWeeks = numOfWeeks;
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
	public int numOfWeeks() {
		return this.builder.numOfWeeks;
	}

	@Override
	public String[] channelAddresses() {
		return this.builder.channelAddresses;
	}

}
