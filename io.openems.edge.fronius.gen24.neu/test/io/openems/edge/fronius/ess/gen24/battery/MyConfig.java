package io.openems.edge.fronius.ess.gen24.battery;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.edge.fronius.ess.gen24.battery.Config;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String modbusId;
		private int modbusUnitId;
		private int chargeMaxVoltage;
		private int dischargeMinVoltage;
		private int numberOfModules;

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

		public Builder setModbusUnitId(int modbusUnitId) {
			this.modbusUnitId = modbusUnitId;
			return this;
		}

		public Builder setChargeMaxVoltage(int chargeMaxVoltage) {
			this.chargeMaxVoltage = chargeMaxVoltage;
			return this;
		}

		public Builder setDischargeMinVoltage(int dischargeMinVoltage) {
			this.dischargeMinVoltage = dischargeMinVoltage;
			return this;
		}

		public Builder numberOfModules(int numberOfModules) {
			this.numberOfModules = numberOfModules;
			return this;
		}

		public MyConfig build() {
			return new MyConfig(this);
		}
	}

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
	public int modbusUnitId() {
		return this.builder.modbusUnitId;
	}

	@Override
	public int chargeMaxVoltage() {
		return this.builder.chargeMaxVoltage;
	}

	@Override
	public int dischargeMinVoltage() {
		return this.builder.dischargeMinVoltage;
	}

	@Override
	public int numberOfModules() {
		return this.builder.numberOfModules;
	}
}
