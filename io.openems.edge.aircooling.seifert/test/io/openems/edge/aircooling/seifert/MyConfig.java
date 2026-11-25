package io.openems.edge.aircooling.seifert;

import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	private final Builder builder;

	private MyConfig(Builder builder) {
		super(Config.class, builder.id);
		this.builder = builder;
	}

	/**
	 * Create a Config builder.
	 * 
	 * @return a {@link Builder}
	 */
	public static Builder create() {
		return new Builder();
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
	public int coolingSetpoint() {
		return this.builder.coolingSetpoint;
	}

	@Override
	public int heatingSetpoint() {
		return this.builder.heatingSetpoint;
	}

	@Override
	public int highTempAlarmSetpoint() {
		return this.builder.highTempAlarmSetpoint;
	}

	@Override
	public int lowTempAlarmSetpoint() {
		return this.builder.lowTempAlarmSetpoint;
	}

	protected static class Builder {
		private String id;
		private String modbusId;
		private int modbusUnitId;
		private int coolingSetpoint;
		private int heatingSetpoint;
		private int highTempAlarmSetpoint;
		private int lowTempAlarmSetpoint;

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

		public Builder setCoolingSetpoint(int coolingSetpoint) {
			this.coolingSetpoint = coolingSetpoint;
			return this;
		}

		public Builder setHeatingSetpoint(int heatingSetpoint) {
			this.heatingSetpoint = heatingSetpoint;
			return this;
		}

		public Builder setHighTempAlarmSetpoint(int highTempAlarmSetpoint) {
			this.highTempAlarmSetpoint = highTempAlarmSetpoint;
			return this;
		}

		public Builder setLowTempAlarmSetpoint(int lowTempAlarmSetpoint) {
			this.lowTempAlarmSetpoint = lowTempAlarmSetpoint;
			return this;
		}

		public MyConfig build() {
			return new MyConfig(this);
		}
	}

}