package io.openems.edge.battery.bmw;

import io.openems.common.utils.ConfigUtils;
import io.openems.edge.battery.bmw.enums.BatteryState;
import io.openems.edge.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id = null;
		private String modbusId = null;
		public int modbusUnitId;
		public BatteryState batteryState;
		public long errorDelay;
		public int maxStartAttempts;
		public int maxStartTime;
		public int startUnsuccessfulDelay;
		public int pendingTolerance;

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

		public Builder setBatteryState(BatteryState batteryState) {
			this.batteryState = batteryState;
			return this;
		}

		public Builder setErrorDelay(long errorDelay) {
			this.errorDelay = errorDelay;
			return this;
		}

		public Builder setMaxStartAttempts(int maxStartAttempts) {
			this.maxStartAttempts = maxStartAttempts;
			return this;
		}

		public Builder setMaxStartTime(int maxStartTime) {
			this.maxStartTime = maxStartTime;
			return this;
		}

		public Builder setStartUnsuccessfulDelay(int startUnsuccessfulDelay) {
			this.startUnsuccessfulDelay = startUnsuccessfulDelay;
			return this;
		}

		public Builder setPendingTolerance(int pendingTolerance) {
			this.pendingTolerance = pendingTolerance;
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
	public int modbusUnitId() {
		return this.builder.modbusUnitId;
	}

	@Override
	public BatteryState batteryState() {
		return this.builder.batteryState;
	}

	@Override
	public long errorDelay() {
		return this.builder.errorDelay;
	}

	@Override
	public int maxStartAttempts() {
		return this.builder.maxStartAttempts;
	}

	@Override
	public int maxStartTime() {
		return this.builder.maxStartTime;
	}

	@Override
	public int startUnsuccessfulDelay() {
		return this.builder.startUnsuccessfulDelay;
	}

	@Override
	public int pendingTolerance() {
		return this.builder.pendingTolerance;
	}

}