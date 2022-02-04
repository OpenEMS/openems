package io.openems.edge.battery.soltaro.single.versionc;

import io.openems.common.utils.ConfigUtils;
import io.openems.edge.battery.soltaro.common.enums.ModuleType;
import io.openems.edge.common.startstop.StartStopConfig;
import io.openems.edge.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id = null;
		private String modbusId = null;
		public int modbusUnitId;
		public int errorLevel2Delay;
		public int maxStartTime;
		public int pendingTolerance;
		public int maxStartAppempts;
		public int startUnsuccessfulDelay;
		public int minimalCellVoltage;
		public StartStopConfig startStop;
		public int numberOfSlaves;
		public ModuleType moduleType;
		public int watchdog;
		public int socLowAlarm;
		public boolean reduceTasks;

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

		public Builder setStartStop(StartStopConfig startStop) {
			this.startStop = startStop;
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
	public StartStopConfig startStop() {
		return this.builder.startStop;
	}
}