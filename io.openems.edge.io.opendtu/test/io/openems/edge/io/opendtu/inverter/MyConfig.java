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
		private String username;
		private String password;
		private String serial;
		private SinglePhase phase;
		private int initialPowerLimit;

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

		public Builder setUsername(String username) {
			this.username = username;
			return this;
		}

		public Builder setPassword(String password) {
			this.password = password;
			return this;
		}

		public Builder setSerial(String serial) {
			this.serial = serial;
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
        return this.builder.username;
    }

    @Override
    public String password() {
        return this.builder.password;
    }

    @Override
    public SinglePhase phase() {
        return this.builder.phase;
    }

    @Override
    public String serialNumber() {
        return this.builder.serial;
    }

    @Override
    public int initialPowerLimit() {
        return this.builder.initialPowerLimit;
    }
}