package io.openems.edge.ess.sinexcel;

import io.openems.common.utils.ConfigUtils;
import io.openems.edge.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id = null;
		private String modbusId = null;
		private int modbusUnitId;
		private String batteryId;
		private int toppingCharge;
		private InverterState inverterState;

		private String digitalInput1 = null;
		private String digitalInput2 = null;
		private String digitalInput3 = null;
		
		private String digitalOutput1 = null;
		private String digitalOutput2 = null;
		private String digitalOutput3 = null;

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

		public Builder setBatteryId(String batteryId) {
			this.batteryId = batteryId;
			return this;
		}

		public Builder setToppingCharge(int toppingCharge) {
			this.toppingCharge = toppingCharge;
			return this;
		}

		public Builder setInverterState(InverterState inverterState) {
			this.inverterState = inverterState;
			return this;
		}
		
		public Builder setDigitalInput1(String digitalInput1) {
			this.digitalInput1 = digitalInput1;
			return this;
		}

		public Builder setDigitalInput2(String digitalInput2) {
			this.digitalInput2 = digitalInput2;
			return this;
		}

		public Builder setDigitalInput3(String digitalInput3) {
			this.digitalInput3 = digitalInput3;
			return this;
		}
		
		public Builder setDigitalOutput1(String digitalOutput1) {
			this.digitalOutput1 = digitalOutput1;
			return this;
		}

		public Builder setDigitalOutput2(String digitalOutput2) {
			this.digitalOutput2 = digitalOutput2;
			return this;
		}

		public Builder setDigitalOutput3(String digitalOutput3) {
			this.digitalOutput3 = digitalOutput3;
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
	public String Modbus_target() {
		return ConfigUtils.generateReferenceTargetFilter(this.id(), this.modbus_id());
	}

	@Override
	public String battery_id() {
		return this.builder.batteryId;
	}

	@Override
	public String Battery_target() {
		return ConfigUtils.generateReferenceTargetFilter(this.id(), this.battery_id());
	}

	@Override
	public int toppingCharge() {
		return this.builder.toppingCharge;
	}

	@Override
	public InverterState InverterState() {
		return this.builder.inverterState;
	}

	@Override
	public String digitalInput1() {
		return this.builder.digitalInput1;
	}

	@Override
	public String digitalInput2() {
		return this.builder.digitalInput2;
	}

	@Override
	public String digitalInput3() {
		return this.builder.digitalInput3;
	}

	@Override
	public String digitalOutput1() {
		return this.builder.digitalOutput1;
	}

	@Override
	public String digitalOutput2() {
		return this.builder.digitalOutput2;
	}

	@Override
	public String digitalOutput3() {
		return this.builder.digitalOutput3;
	}

}