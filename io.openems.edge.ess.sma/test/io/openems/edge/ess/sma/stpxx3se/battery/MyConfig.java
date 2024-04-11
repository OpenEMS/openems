package io.openems.edge.ess.sma.stpxx3se.battery;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.utils.ConfigUtils;
import io.openems.edge.ess.sma.stpxx3se.battery.MyConfig.Builder;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String modbusId;
		private int modbusUnitId;
		private int chargeMaxVoltage;
		private int dischargeMinVoltage;

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
		return builder.modbusId;
	}

	@Override
	public int modbusUnitId() {
		return builder.modbusUnitId;
	}

	@Override
	public String Modbus_target() {
		return ConfigUtils.generateReferenceTargetFilter(this.id(), this.modbus_id());
	}

	@Override
	public int chargeMaxVoltage() {
		return this.builder.chargeMaxVoltage;
	}

	@Override
	public int dischargeMinVoltage() {
		return this.builder.dischargeMinVoltage;
	}
}
