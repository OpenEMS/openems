package io.openems.edge.heat.askoma;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.utils.ConfigUtils;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String modbusId = null;
		private int modbusUnitId;
		private boolean readonly = false;
		private Mode mode = Mode.OFF;
		private String jsCalendar = "[]";
		private int maxHeatPower;

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

		public Builder setReadOnly(boolean readOnly) {
			this.readonly = readOnly;
			return this;
		}

		public Builder setMode(Mode mode) {
			this.mode = mode;
			return this;
		}

		public Builder setJsCalendar(String jsCalendar) {
			this.jsCalendar = jsCalendar;
			return this;
		}

		public Builder setMaxHeatPower(int maxHeatPower) {
			this.maxHeatPower = maxHeatPower;
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
	public int modbusUnitId() {
		return this.builder.modbusUnitId;
	}

	@Override
	public boolean readOnly() {
		return this.builder.readonly;
	}

	@Override
	public Mode mode() {
		return this.builder.mode;
	}

	@Override
	public String jsCalendar() {
		return this.builder.jsCalendar;
	}

	@Override
	public int maxHeatPower() {
		return this.builder.maxHeatPower;
	}

	@Override
	public String Modbus_target() {
		return ConfigUtils.generateReferenceTargetFilter(this.id(), this.modbus_id());
	}

}