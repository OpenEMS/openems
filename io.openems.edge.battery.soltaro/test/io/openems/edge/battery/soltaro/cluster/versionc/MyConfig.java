package io.openems.edge.battery.soltaro.cluster.versionc;

import io.openems.common.utils.ConfigUtils;
import io.openems.edge.battery.soltaro.ModuleType;
import io.openems.edge.common.startstop.StartStopConfig;
import io.openems.edge.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id = null;
		private String modbusId = null;
		public int modbusUnitId;
		public StartStopConfig startStop;
		public int numberOfSlaves;
		public ModuleType moduleType;
		public boolean isRack1Used;
		public boolean isRack2Used;
		public boolean isRack3Used;
		public boolean isRack4Used;
		public boolean isRack5Used;
		public int watchdog;

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

		public Builder setStartStop(StartStopConfig startStop) {
			this.startStop = startStop;
			return this;
		}

		public Builder setNumberOfSlaves(int numberOfSlaves) {
			this.numberOfSlaves = numberOfSlaves;
			return this;
		}

		public Builder setModuleType(ModuleType moduleType) {
			this.moduleType = moduleType;
			return this;
		}

		public Builder setRack1Used(boolean isRack1Used) {
			this.isRack1Used = isRack1Used;
			return this;
		}

		public Builder setRack2Used(boolean isRack2Used) {
			this.isRack2Used = isRack2Used;
			return this;
		}

		public Builder setRack3Used(boolean isRack3Used) {
			this.isRack3Used = isRack3Used;
			return this;
		}

		public Builder setRack4Used(boolean isRack4Used) {
			this.isRack4Used = isRack4Used;
			return this;
		}

		public Builder setRack5Used(boolean isRack5Used) {
			this.isRack5Used = isRack5Used;
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

	@Override
	public int numberOfSlaves() {
		return this.builder.numberOfSlaves;
	}

	@Override
	public ModuleType moduleType() {
		return this.builder.moduleType;
	}

	@Override
	public boolean isRack1Used() {
		return this.builder.isRack1Used;
	}

	@Override
	public boolean isRack2Used() {
		return this.builder.isRack2Used;
	}

	@Override
	public boolean isRack3Used() {
		return this.builder.isRack3Used;
	}

	@Override
	public boolean isRack4Used() {
		return this.builder.isRack4Used;
	}

	@Override
	public boolean isRack5Used() {
		return this.builder.isRack5Used;
	}

	@Override
	public int watchdog() {
		return this.builder.watchdog;
	}

}