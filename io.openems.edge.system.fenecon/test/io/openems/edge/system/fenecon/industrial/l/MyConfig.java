package io.openems.edge.system.fenecon.industrial.l;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.utils.ConfigUtils;
import io.openems.edge.system.fenecon.industrial.l.envicool.Envicool;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String[] batteryIds;
		private String acModbusId = null;
		private Envicool.Mode acMode;
		private int acModbusUnitId;
		private int acCoolingSetPoint;
		private int acHeatingSetPoint;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setBatteryIds(String... batteryIds) {
			this.batteryIds = batteryIds;
			return this;
		}

		public Builder setAcModbusId(String acModbusId) {
			this.acModbusId = acModbusId;
			return this;
		}

		public Builder setAcMode(Envicool.Mode acMode) {
			this.acMode = acMode;
			return this;
		}

		public Builder setAcModbusUnitId(int acModbusUnitId) {
			this.acModbusUnitId = acModbusUnitId;
			return this;
		}

		public Builder setAcCoolingSetPoint(int acCoolingSetPoint) {
			this.acCoolingSetPoint = acCoolingSetPoint;
			return this;
		}

		public Builder setAcHeatingSetPoint(int acHeatingSetPoint) {
			this.acHeatingSetPoint = acHeatingSetPoint;
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
	public String[] batteryIds() {
		return this.builder.batteryIds;
	}

	@Override
	public String acModbus_id() {
		return this.builder.acModbusId;
	}

	@Override
	public String AcModbus_target() {
		return ConfigUtils.generateReferenceTargetFilter(this.id(), this.acModbus_id());
	}

	@Override
	public int acModbusUnitId() {
		return this.builder.acModbusUnitId;
	}

	@Override
	public Envicool.Mode acMode() {
		return this.builder.acMode;
	}

	@Override
	public int acCoolingSetPoint() {
		return this.builder.acCoolingSetPoint;
	}

	@Override
	public int acHeatingSetPoint() {
		return this.builder.acHeatingSetPoint;
	}
}