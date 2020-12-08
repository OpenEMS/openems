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
		private int EmsTimeout;
		private int BmsTimeout;

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
		
		public Builder setEmsTimeout(int EmsTimeout) {
			this.EmsTimeout = EmsTimeout;
			return this;
		}
		
		public Builder setBmsTimeout(int BmsTimeout) {
			this.BmsTimeout = BmsTimeout;
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
	public int Ems_timeout() {
		return this.builder.EmsTimeout;
	}

	@Override
	public int Bms_timeout() {
		return this.builder.BmsTimeout;
	}

}