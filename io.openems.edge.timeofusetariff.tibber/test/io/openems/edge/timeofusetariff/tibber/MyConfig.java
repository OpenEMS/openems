package io.openems.edge.timeofusetariff.tibber;

import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	public static class Builder {
		private String id;
		public String accessToken;
		public String filter;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setAccessToken(String accessToken) {
			this.accessToken = accessToken;
			return this;
		}

		public Builder setFilter(String filter) {
			this.filter = filter;
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
	public String accessToken() {
		return this.builder.accessToken;
	}

	@Override
	public String filter() {
		return this.builder.filter;
	}

}