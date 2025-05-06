package io.openems.edge.kostal.plenticore.ess;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.types.MeterType;
import io.openems.common.utils.ConfigUtils;
import io.openems.edge.kostal.plenticore.enums.ControlMode;
import io.openems.edge.kostal.plenticore.ess.Config;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {

		private String id;
		private boolean readOnlyMode;
		private String modbusId;
		private int modbusUnitId;
		private int capacity;
		private String ctrlId;
		private String ctrlTarget;

		private MeterType type;
		private int minsoc;
		private int watchdog;
		private int tolerance;
		private boolean debugMode;
		private ControlMode controlMode;

		private Builder() {
			// empty
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setReadOnlyMode(boolean readOnly) {
			this.readOnlyMode = readOnly;
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

		public Builder setTolerance(int tolerance) {
			this.tolerance = tolerance;
			return this;
		}
		
		public Builder setMinSoc(int minsoc) {
			this.minsoc = minsoc;
			return this;
		}

		public Builder setCtrlId(String ctrlId) {
			this.ctrlId = ctrlId;
			return this;
		}

		public Builder setCtrlTarget(String ctrlTarget) {
			this.ctrlTarget = ctrlTarget;
			return this;
		}

		public Builder setDebugMode(boolean debugMode) {
			this.debugMode = debugMode;
			return this;
		}

		public Builder setControlMode(ControlMode controlMode) {
			this.controlMode = controlMode;
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

	@Override
	public int minsoc() {
		return this.builder.minsoc;
	}

	@Override
	public int watchdog() {
		return this.builder.watchdog;
	}

	@Override
	public boolean debugMode() {
		return this.builder.debugMode;
	}

	@Override
	public ControlMode controlMode() {
		return this.builder.controlMode;
	}
	
	@Override
	public int tolerance() {
		return this.builder.tolerance;
	}

}
