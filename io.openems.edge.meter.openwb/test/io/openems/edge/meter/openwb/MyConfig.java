package io.openems.edge.meter.openwb;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.types.MeterType;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String ipAddress;
		private int port;
		private MeterType type;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setIp(String ip) {
			this.ipAddress = ip;
			return this;
		}

		public Builder setPort(int port) {
			this.port = port;
			return this;
		}

		public Builder setType(MeterType type) {
			this.type = type;
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
	public MeterType type() {
		return this.builder.type;
	}

	@Override
	public int port() {
		return this.builder.port;
	}
}