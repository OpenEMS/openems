package io.openems.edge.io.gpio;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.edge.io.gpio.hardware.HardwareType;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String alias;
		private boolean enabled;
		private String gpioPath;
		private HardwareType hardwareType;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setAlias(String alias) {
			this.alias = alias;
			return this;
		}

		public Builder setEnabled(boolean enabled) {
			this.enabled = enabled;
			return this;
		}

		public Builder setGpioPath(String gpioPath) {
			this.gpioPath = gpioPath;
			return this;
		}

		public Builder setHardwareType(HardwareType hardwareType) {
			this.hardwareType = hardwareType;
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
	public String id() {
		return this.builder.id;
	}

	@Override
	public String alias() {
		return this.builder.alias;
	}

	@Override
	public boolean enabled() {
		return this.builder.enabled;
	}

	@Override
	public String gpioPath() {
		return this.builder.gpioPath;
	}

	@Override
	public HardwareType hardwareType() {
		return this.builder.hardwareType;
	}
}