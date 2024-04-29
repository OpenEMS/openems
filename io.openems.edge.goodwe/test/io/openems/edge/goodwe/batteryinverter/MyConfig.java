package io.openems.edge.goodwe.batteryinverter;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.utils.ConfigUtils;
import io.openems.edge.goodwe.common.enums.ControlMode;
import io.openems.edge.goodwe.common.enums.EnableDisable;
import io.openems.edge.goodwe.common.enums.FeedInPowerSettings;
import io.openems.edge.goodwe.common.enums.SafetyCountry;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	public static class Builder {
		private String id;
		private ControlMode controlMode;
		private String modbusId;
		private int modbusUnitId;
		private SafetyCountry safetyCountry;
		private EnableDisable mpptForShadowEnable;
		private EnableDisable backupEnable;
		private EnableDisable feedPowerEnable;
		private int feedPowerPara;
		private FeedInPowerSettings feedInPowerSettings;
		private EnableDisable rcrEnable = EnableDisable.DISABLE;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setControlMode(ControlMode controlMode) {
			this.controlMode = controlMode;
			return this;
		}

		public Builder setModbusId(String modbusId) {
			this.modbusId = modbusId;
			return this;
		}

		public Builder setModbusUnitId(int modbusUnitId) {
			this.modbusUnitId = modbusUnitId;
			return this;
		}

		public Builder setSafetyCountry(SafetyCountry safetyCountry) {
			this.safetyCountry = safetyCountry;
			return this;
		}

		public Builder setMpptForShadowEnable(EnableDisable mpptForShadowEnable) {
			this.mpptForShadowEnable = mpptForShadowEnable;
			return this;
		}

		public Builder setBackupEnable(EnableDisable backupEnable) {
			this.backupEnable = backupEnable;
			return this;
		}

		public Builder setFeedPowerEnable(EnableDisable feedPowerEnable) {
			this.feedPowerEnable = feedPowerEnable;
			return this;
		}

		public Builder setFeedPowerPara(int feedPowerPara) {
			this.feedPowerPara = feedPowerPara;
			return this;
		}

		public Builder setFeedInPowerSettings(FeedInPowerSettings feedInPowerSettings) {
			this.feedInPowerSettings = feedInPowerSettings;
			return this;
		}

		public Builder setRcrEnable(EnableDisable rcrEnable) {
			this.rcrEnable = rcrEnable;
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
	public int modbusUnitId() {
		return this.builder.modbusUnitId;
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
	public SafetyCountry safetyCountry() {
		return this.builder.safetyCountry;
	}

	@Override
	public EnableDisable mpptForShadowEnable() {
		return this.builder.mpptForShadowEnable;
	}

	@Override
	public EnableDisable backupEnable() {
		return this.builder.backupEnable;
	}

	@Override
	public EnableDisable feedPowerEnable() {
		return this.builder.feedPowerEnable;
	}

	@Override
	public int feedPowerPara() {
		return this.builder.feedPowerPara;
	}

	@Override
	public FeedInPowerSettings setfeedInPowerSettings() {
		return this.builder.feedInPowerSettings;
	}

	@Override
	public ControlMode controlMode() {
		return this.builder.controlMode;
	}

	@Override
	public EnableDisable rcrEnable() {
		return this.builder.rcrEnable;
	}
}