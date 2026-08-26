package io.openems.edge.evcs.abl;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.utils.ConfigUtils;
import io.openems.edge.meter.api.PhaseRotation;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private boolean readOnly;
		private String id;
		private Plug plug;
		private String modbusId;
		private int modbusUnitId;
		private int minHwCurrent;
		private int maxHwCurrent;
		private PhaseRotation phaseRotation;
		private boolean debugMode;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setPlug(Plug plug) {
			this.plug = plug;
			return this;
		}

		public Builder setModbusId(String id) {
			this.modbusId = id;
			return this;
		}

		public Builder setModbusUnitId(int unitId) {
			this.modbusUnitId = unitId;
			return this;
		}

		public Builder setMinHwCurrent(int current) {
			this.minHwCurrent = current;
			return this;
		}

		public Builder setMaxHwCurrent(int current) {
			this.maxHwCurrent = current;
			return this;
		}

		public Builder setPhaseRotation(PhaseRotation rot) {
			this.phaseRotation = rot;
			return this;
		}

		public Builder setReadOnly(boolean readOnly) {
			this.readOnly = readOnly;
			return this;
		}

		public Builder setDebugMode(boolean debugMode) {
			this.debugMode = debugMode;
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
	public Plug plug() {
		return this.builder.plug;
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
	public int minHwCurrent() {
		return this.builder.minHwCurrent;
	}

	@Override
	public int maxHwCurrent() {
		return this.builder.maxHwCurrent;
	}

	@Override
	public PhaseRotation phaseRotation() {
		return this.builder.phaseRotation;
	}

	@Override
	public boolean readOnly() {
		return this.builder.readOnly;
	}

	@Override
	public boolean debugMode() {
		return this.builder.debugMode;
	}

	@Override
	public String Modbus_target() {
		return ConfigUtils.generateReferenceTargetFilter(this.id(), this.modbus_id());
	}

}
