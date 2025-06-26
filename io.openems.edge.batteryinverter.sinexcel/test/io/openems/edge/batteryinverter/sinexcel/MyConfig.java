package io.openems.edge.batteryinverter.sinexcel;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.utils.ConfigUtils;
import io.openems.edge.batteryinverter.sinexcel.enums.CountryCode;
import io.openems.edge.batteryinverter.sinexcel.enums.EnableDisable;
import io.openems.edge.common.startstop.StartStopConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id = null;
		private String modbusId = null;
		private StartStopConfig startStopConfig = null;
		private CountryCode countryCode = null;
		private EnableDisable emergencyPower = null;

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

		public Builder setCountryCode(CountryCode countryCode) {
			this.countryCode = countryCode;
			return this;
		}

		public Builder setEmergencyPower(EnableDisable emergencyPower) {
			this.emergencyPower = emergencyPower;
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
	public CountryCode countryCode() {
		return this.builder.countryCode;
	}

	@Override
	public EnableDisable emergencyPower() {
		return this.builder.emergencyPower;
	}

	@Override
	public String Modbus_target() {
		return ConfigUtils.generateReferenceTargetFilter(this.id(), this.modbus_id());
	}

}