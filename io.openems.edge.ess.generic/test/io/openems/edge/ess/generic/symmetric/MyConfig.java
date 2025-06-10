package io.openems.edge.ess.generic.symmetric;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.utils.ConfigUtils;
import io.openems.edge.common.startstop.StartStopConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id = null;
		private StartStopConfig startStopConfig = null;
		private String batteryInverterId = null;
		private String batteryId = null;

		private Builder() {

		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setStartStopConfig(StartStopConfig startStopConfig) {
			this.startStopConfig = startStopConfig;
			return this;
		}

		public Builder setBatteryInverterId(String batteryInverterId) {
			this.batteryInverterId = batteryInverterId;
			return this;
		}

		public Builder setBatteryId(String batteryId) {
			this.batteryId = batteryId;
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
	public StartStopConfig startStop() {
		return this.builder.startStopConfig;
	}

	@Override
	public String batteryInverter_id() {
		return this.builder.batteryInverterId;
	}

	@Override
	public String battery_id() {
		return this.builder.batteryId;
	}

	@Override
	public String batteryInverter_target() {
		return ConfigUtils.generateReferenceTargetFilter(this.id(), this.batteryInverter_id());
	}

	@Override
	public String battery_target() {
		return ConfigUtils.generateReferenceTargetFilter(this.id(), this.battery_id());
	}

}