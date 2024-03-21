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
		private String serialNumber;
		private SinglePhase phase;
		private int initialPowerLimit;
		private String username;
		private String password;

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

		public Builder setSerialNumber(String serialNumber) {
			this.serialNumber = serialNumber;
			return this;
		}

		public Builder setPhase(SinglePhase phase) {
			this.phase = phase;
			return this;
		}

		public Builder setInitialPowerLimit(int initialPowerLimit) {
			this.initialPowerLimit = initialPowerLimit;
			return this;
		}

		public Builder setUsername(String username) {
			this.username = username;
			return this;
		}

		public Builder setPassword(String password) {
			this.password = password;
			return this;
		}

		public MyConfig build() {
			return new MyConfig(this);
		}
	}

	private final String ip;
	private final MeterType type;
	private final String serialNumber;
	private final SinglePhase phase;
	private final int initialPowerLimit;
	private final String username;
	private final String password;

	private MyConfig(Builder builder) {
		super(Config.class, builder.id);
		this.ip = builder.ip;
		this.type = builder.type;
		this.serialNumber = builder.serialNumber;
		this.phase = builder.phase;
		this.initialPowerLimit = builder.initialPowerLimit;
		this.username = builder.username;
		this.password = builder.password;
	}

	@Override
	public String ip() {
		return this.ip;
	}

	@Override
	public MeterType type() {
		return this.type;
	}

	@Override
	public String username() {
		return this.username;
	}

	@Override
	public String password() {
		return this.password;
	}

	@Override
	public SinglePhase phase() {
		return this.phase;
	}

	@Override
	public String serialNumber() {
		return this.serialNumber;
	}

	@Override
	public int initialPowerLimit() {
		return this.initialPowerLimit;
	}

	/**
	 * Create a Config builder.
	 *
	 * @return a {@link Builder}
	 */
	public static Builder create() {
		return new Builder();
	}
}
