package io.openems.edge.ess.fenecon.commercial40;

import io.openems.common.utils.ConfigUtils;
import io.openems.edge.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	public static class Builder {
		private String id = null;
		private String modbusId = null;
		public int modbusUnitId;
		public int powerLimitOnPowerDecreaseCausedByOvertemperatureChannel;
		public boolean readOnlyMode;
		public int surplusFeedInSocLimit;
		public int surplusFeedInAllowedChargePowerLimit;
		public double surplusFeedInIncreasePowerFactor;
		public int surplusFeedInMaxIncreasePowerFactor;
		public int surplusFeedInPvLimitOnPowerDecreaseCausedByOvertemperature;
		public String surplusFeedInOffTime;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setModbusId(String modbusId) {
			this.modbusId = modbusId;
			return this;
		}

		public Builder setPowerLimitOnPowerDecreaseCausedByOvertemperatureChannel(
				int powerLimitOnPowerDecreaseCausedByOvertemperatureChannel) {
			this.powerLimitOnPowerDecreaseCausedByOvertemperatureChannel = powerLimitOnPowerDecreaseCausedByOvertemperatureChannel;
			return this;
		}

		public Builder setReadOnlyMode(boolean readOnlyMode) {
			this.readOnlyMode = readOnlyMode;
			return this;
		}

		public Builder setSurplusFeedInSocLimit(int socLimit) {
			this.surplusFeedInSocLimit = socLimit;
			return this;
		}

		public Builder setSurplusFeedInAllowedChargePowerLimit(int allowedChargePowerLimit) {
			this.surplusFeedInAllowedChargePowerLimit = allowedChargePowerLimit;
			return this;
		}

		public Builder setSurplusFeedInIncreasePowerFactor(double increasePowerFactor) {
			this.surplusFeedInIncreasePowerFactor = increasePowerFactor;
			return this;
		}

		public Builder setSurplusFeedInMaxIncreasePowerFactor(int maxIncreasePowerFactor) {
			this.surplusFeedInMaxIncreasePowerFactor = maxIncreasePowerFactor;
			return this;
		}

		public Builder setSurplusFeedInPvLimitOnPowerDecreaseCausedByOvertemperature(
				int pvLimitOnPowerDecreaseCausedByOvertemperature) {
			this.surplusFeedInPvLimitOnPowerDecreaseCausedByOvertemperature = pvLimitOnPowerDecreaseCausedByOvertemperature;
			return this;
		}

		public Builder setSurplusFeedInOffTime(String offTime) {
			this.surplusFeedInOffTime = offTime;
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
	public String modbus_id() {
		return this.builder.modbusId;
	}

	@Override
	public String Modbus_target() {
		return ConfigUtils.generateReferenceTargetFilter(this.id(), this.modbus_id());
	}

	@Override
	public boolean readOnlyMode() {
		return this.builder.readOnlyMode;
	}

	@Override
	public int powerLimitOnPowerDecreaseCausedByOvertemperatureChannel() {
		return this.builder.powerLimitOnPowerDecreaseCausedByOvertemperatureChannel;
	}

	@Override
	public int surplusFeedInSocLimit() {
		return this.builder.surplusFeedInSocLimit;
	}

	@Override
	public int surplusFeedInAllowedChargePowerLimit() {
		return this.builder.surplusFeedInAllowedChargePowerLimit;
	}

	@Override
	public double surplusFeedInIncreasePowerFactor() {
		return this.builder.surplusFeedInIncreasePowerFactor;
	}

	@Override
	public int surplusFeedInMaxIncreasePowerFactor() {
		return this.builder.surplusFeedInMaxIncreasePowerFactor;
	}

	@Override
	public int surplusFeedInPvLimitOnPowerDecreaseCausedByOvertemperature() {
		return this.builder.surplusFeedInPvLimitOnPowerDecreaseCausedByOvertemperature;
	}

	@Override
	public String surplusFeedInOffTime() {
		return this.builder.surplusFeedInOffTime;
	}

}