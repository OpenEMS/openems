package io.openems.edge.io.opendtu.inverter;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SinglePhase;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String ip;
		private MeterType type;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setIp(String ip) {
			this.ip = ip;
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
	public String ip() {
		return this.builder.ip;
	}

	@Override
	public MeterType type() {
		return this.builder.type;
	}

	@Override
	public String username() {
		return "admin";
	}

	@Override
	public String password() {
		return "admin";
	}

	@Override
	public SinglePhase phase() {
		return SinglePhase.L1;
	}

	@Override
	public String serialNumber() {
		return "932932";
	}

	@Override
	public int initialPowerLimit() {
		return 100;
	}
}