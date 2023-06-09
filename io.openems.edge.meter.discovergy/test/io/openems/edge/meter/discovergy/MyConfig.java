package io.openems.edge.meter.discovergy;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.edge.meter.api.MeterType;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id = null;
		private MeterType type;
		private String email;
		private String password;
		private String meterId;
		private String serialNumber;
		private String fullSerialNumber;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setType(MeterType type) {
			this.type = type;
			return this;
		}

		public Builder setPassword(String password) {
			this.password = password;
			return this;
		}

		public Builder setMeterId(String meterId) {
			this.meterId = meterId;
			return this;
		}

		public Builder setEmail(String email) {
			this.email = email;
			return this;
		}

		public Builder setFullSerialNumber(String fullSerialNumber) {
			this.fullSerialNumber = fullSerialNumber;
			return this;
		}

		public Builder setSerialNumber(String serialNumber) {
			this.serialNumber = serialNumber;
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
	public MeterType type() {
		return this.builder.type;
	}

	@Override
	public String email() {
		return this.builder.email;
	}

	@Override
	public String password() {
		return this.builder.password;
	}

	@Override
	public String meterId() {
		return this.builder.meterId;
	}

	@Override
	public String serialNumber() {
		return this.builder.serialNumber;
	}

	@Override
	public String fullSerialNumber() {
		return this.builder.fullSerialNumber;
	}
}