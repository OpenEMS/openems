package io.openems.edge.batteryinverter.kaco.blueplanetgridsave;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.utils.ConfigUtils;
import io.openems.edge.common.startstop.StartStopConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id = null;
		private StartStopConfig startStopConfig = null;
		private String modbusId = null;
		private boolean activateWatchdog;

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

		public Builder setModbusId(String modbusId) {
			this.modbusId = modbusId;
			return this;
		}

		public Builder setActivateWatchdog(boolean activateWatchdog) {
			this.activateWatchdog = activateWatchdog;
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
	public String modbus_id() {
		return this.builder.modbusId;
	}

	@Override
	public String Modbus_target() {
		return ConfigUtils.generateReferenceTargetFilter(this.id(), this.modbus_id());
	}

	@Override
	public boolean activateWatchdog() {
		return this.builder.activateWatchdog;
	}
}