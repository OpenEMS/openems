package io.openems.edge.goodwe.charger;

import io.openems.common.utils.ConfigUtils;
import io.openems.edge.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements ConfigPV1, ConfigPV2 {

	public static class Builder {
		private String id = null;
		public String essOrBatteryInverter;
		public int unitId;
		public String modbusId;

		private Builder() {

		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setBatteryInverterId(String essOrBatteryInverter) {
			this.essOrBatteryInverter = essOrBatteryInverter;
			return this;
		}

		public Builder setModbusId(String modbusId) {
			this.modbusId = modbusId;
			return this;
		}

		public Builder setUnitId(int unitId) {
			this.unitId = unitId;
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
		super(ConfigPV1.class, builder.id);
		this.builder = builder;
	}

	@Override
	public int unit_id() {
		return this.builder.unitId;
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
	public String essOrBatteryInverter_id() {
		return this.builder.essOrBatteryInverter;
	}

	@Override
	public String essOrBatteryInverter_target() {
		return ConfigUtils.generateReferenceTargetFilter(this.id(), this.essOrBatteryInverter_id());
	}

}