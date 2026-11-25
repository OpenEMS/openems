package io.openems.edge.gassensor.vigilex;

import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String modbusId = null;
		private int modbusUnitId;
		private int hydrogenRe1LowSetpoint = 500;
		private int hydrogenRe1HighSetpoint = 750;
		private int hydrogenRe2LowSetpoint = 1250;
		private int hydrogenRe2HighSetpoint = 2000;
		private int vocRe1LowSetpoint = 150;
		private int vocRe1HighSetpoint = 200;
		private int vocRe2LowSetpoint = 250;
		private int vocRe2HighSetpoint = 400;

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

		public Builder setHydrogenRe1LowSetpoint(int value) {
			this.hydrogenRe1LowSetpoint = value;
			return this;
		}

		public Builder setHydrogenRe1HighSetpoint(int value) {
			this.hydrogenRe1HighSetpoint = value;
			return this;
		}

		public Builder setHydrogenRe2LowSetpoint(int value) {
			this.hydrogenRe2LowSetpoint = value;
			return this;
		}

		public Builder setHydrogenRe2HighSetpoint(int value) {
			this.hydrogenRe2HighSetpoint = value;
			return this;
		}

		public Builder setVocRe1LowSetpoint(int value) {
			this.vocRe1LowSetpoint = value;
			return this;
		}

		public Builder setVocRe1HighSetpoint(int value) {
			this.vocRe1HighSetpoint = value;
			return this;
		}

		public Builder setVocRe2LowSetpoint(int value) {
			this.vocRe2LowSetpoint = value;
			return this;
		}

		public Builder setVocRe2HighSetpoint(int value) {
			this.vocRe2HighSetpoint = value;
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
	public int hydrogen_re1_low_setpoint() {
		return this.builder.hydrogenRe1LowSetpoint;
	}

	@Override
	public int hydrogen_re1_high_setpoint() {
		return this.builder.hydrogenRe1HighSetpoint;
	}

	@Override
	public int hydrogen_re2_low_setpoint() {
		return this.builder.hydrogenRe2LowSetpoint;
	}

	@Override
	public int hydrogen_re2_high_setpoint() {
		return this.builder.hydrogenRe2HighSetpoint;
	}

	@Override
	public int voc_re1_low_setpoint() {
		return this.builder.vocRe1LowSetpoint;
	}

	@Override
	public int voc_re1_high_setpoint() {
		return this.builder.vocRe1HighSetpoint;
	}

	@Override
	public int voc_re2_low_setpoint() {
		return this.builder.vocRe2LowSetpoint;
	}

	@Override
	public int voc_re2_high_setpoint() {
		return this.builder.vocRe2HighSetpoint;
	}

}