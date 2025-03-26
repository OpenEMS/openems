package io.openems.edge.kostal.ess;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.types.MeterType;
import io.openems.common.utils.ConfigUtils;
import io.openems.edge.kostal.enums.ControlMode;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {

		private String id;
		private boolean readOnlyMode;
		private String modbusId;
		private int modbusUnitId;
		private int capacity;
		// private String inverter_id;
		private String core_target;
		private MeterType type;
		public int minsoc;
		public int watchdog;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setReadOnlyMode(boolean readOnly) {
			this.readOnlyMode = readOnlyMode;
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

		public Builder setCapacity(int capacity) {
			this.capacity = capacity;
			return this;
		}

		public Builder setWatchdog(int watchdog) {
			this.watchdog = watchdog;
			return this;
		}

		public Builder setMinSoc(int minsoc) {
			this.minsoc = minsoc;
			return this;
		}

		// public Builder setInverter_id(String inverter_id) {
		// this.inverter_id = inverter_id;
		// return this;
		// }

		public Builder setCore_target(String core_target) {
			this.core_target = core_target;
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
	public boolean readOnlyMode() {
		return this.builder.readOnlyMode;
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
	public int capacity() {
		return this.builder.capacity;
	}

	// @Override
	// public String inverter_id() {
	// return this.builder.inverter_id;
	// }

	@Override
	public String core_target() {
		return this.builder.core_target;
	}

	@Override
	public ControlMode controlMode() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int minsoc() {
		return this.builder.minsoc;
	}

	@Override
	public int watchdog() {
		return this.builder.watchdog;
	}
}
