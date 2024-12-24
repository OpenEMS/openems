package io.openems.edge.timeofusetariff.rabotcharge;

import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	public static class Builder {
		private String id;
		private String zipcode;
		private String clientId;
		private String clientSecret;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setZipcode(String zipcode) {
			this.zipcode = zipcode;
			return this;
		}

		public Builder setClientId(String clientId) {
			this.clientId = clientId;
			return this;
		}

		public Builder setClientSecret(String clientSecret) {
			this.clientSecret = clientSecret;
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
	public String zipcode() {
		return this.builder.zipcode;
	}

	@Override
	public String clientId() {
		return this.builder.clientId;
	}

	@Override
	public String clientSecret() {
		return this.builder.clientSecret;
	}

}