package io.openems.edge.goodwe.gridmeter;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.utils.ConfigUtils;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	public static class Builder {
		private String id;
		private int modbusUnitId;
		private String modbusId;
		private GoodWeGridMeterCategory goodWeMeterCategory;
		private int externalMeterRatioValueA;
		private int externalMeterRatioValueB;

		private Builder() {
		}

		protected Builder setId(String id) {
			this.id = id;
			return this;
		}

		protected Builder setModbusId(String modbusId) {
			this.modbusId = modbusId;
			return this;
		}

		protected Builder setModbusUnitId(int modbusUnitId) {
			this.modbusUnitId = modbusUnitId;
			return this;
		}

		protected Builder setGoodWeMeterCategory(GoodWeGridMeterCategory goodWeMeterCategory) {
			this.goodWeMeterCategory = goodWeMeterCategory;
			return this;
		}

		protected Builder setExternalMeterRatioValueA(int externalMeterRatioValueA) {
			this.externalMeterRatioValueA = externalMeterRatioValueA;
			return this;
		}

		protected Builder setExternalMeterRatioValueB(int externalMeterRatioValueB) {
			this.externalMeterRatioValueB = externalMeterRatioValueB;
			return this;
		}

		protected MyConfig build() {
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
	public GoodWeGridMeterCategory goodWeMeterCategory() {
		return this.builder.goodWeMeterCategory;
	}

	@Override
	public int externalMeterRatioValueA() {
		return this.builder.externalMeterRatioValueA;
	}

	@Override
	public int externalMeterRatioValueB() {
		return this.builder.externalMeterRatioValueB;
	}

}