package io.openems.edge.goodwe.charger.twostring;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.utils.ConfigUtils;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	public static class Builder {
		private String id;
		private String essOrBatteryInverter;
		private PvPort pvPort;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setBatteryInverterId(String essOrBatteryInverter) {
			this.essOrBatteryInverter = essOrBatteryInverter;
			return this;
		}

		public Builder setPvPort(PvPort pvPort) {
			this.pvPort = pvPort;
			return this;
		}

		/**
		 * Builds the Config.
		 *
		 * @return the Config
		 */
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
	public String essOrBatteryInverter_id() {
		return this.builder.essOrBatteryInverter;
	}

	@Override
	public PvPort pvPort() {
		return this.builder.pvPort;
	}

	@Override
	public String essOrBatteryInverter_target() {
		return ConfigUtils.generateReferenceTargetFilter(this.id(), this.essOrBatteryInverter_id());
	}
}
