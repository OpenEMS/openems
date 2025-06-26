package io.openems.edge.tesla.powerwall2.core;

import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String ipAddress;
		private int port;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setIpAddress(String ipAddress) {
			this.ipAddress = ipAddress;
			return this;
		}

		public Builder setPort(int port) {
			this.port = port;
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
	public String ipAddress() {
		return this.builder.ipAddress;
	}

	@Override
	public int port() {
		return this.builder.port;
	}
}