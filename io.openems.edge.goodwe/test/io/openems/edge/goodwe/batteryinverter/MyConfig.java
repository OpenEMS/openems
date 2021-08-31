package io.openems.edge.goodwe.batteryinverter;

import io.openems.common.utils.ConfigUtils;
import io.openems.edge.common.test.AbstractComponentConfig;
import io.openems.edge.goodwe.common.enums.BackupEnable;
import io.openems.edge.goodwe.common.enums.ControlMode;
import io.openems.edge.goodwe.common.enums.EmsPowerMode;
import io.openems.edge.goodwe.common.enums.FeedInPowerSettings;
import io.openems.edge.goodwe.common.enums.FeedPowerEnable;
import io.openems.edge.goodwe.common.enums.SafetyCountry;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id = null;
		public ControlMode controlMode;
		public String modbusId;
		public int modbusUnitId;
		public SafetyCountry safetyCountry;
		public BackupEnable backupEnable;
		public FeedPowerEnable feedPowerEnable;
		public int feedPowerPara;
		public FeedInPowerSettings feedInPowerSettings;
		public EmsPowerMode emsPowerMode;
		public int emsPowerSet;

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

		public Builder setBackupEnable(BackupEnable backupEnable) {
			this.backupEnable = backupEnable;
			return this;
		}

		public Builder setFeedPowerEnable(FeedPowerEnable feedPowerEnable) {
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

		public Builder setEmsPowerMode(EmsPowerMode emsPowerMode) {
			this.emsPowerMode = emsPowerMode;
			return this;
		}

		public Builder setEmsPowerSet(int emsPowerSet) {
			this.emsPowerSet = emsPowerSet;
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
	public BackupEnable backupEnable() {
		return this.builder.backupEnable;
	}

	@Override
	public FeedPowerEnable feedPowerEnable() {
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
	public EmsPowerMode emsPowerMode() {
		return this.builder.emsPowerMode;
	}

	@Override
	public int emsPowerSet() {
		return this.builder.emsPowerSet;
	}

	@Override
	public ControlMode controlMode() {
		return this.builder.controlMode;
	}

}