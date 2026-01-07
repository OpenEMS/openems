package io.openems.edge.victron.batteryinverter;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.utils.ConfigUtils;
import io.openems.edge.common.startstop.StartStopConfig;
import io.openems.edge.common.type.Phase.SingleOrAllPhase;
import io.openems.edge.victron.enums.DeviceType;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String alias;
		private boolean enabled;
		private String modbusId;
		private int modbusUnitId;
		private SingleOrAllPhase phase;
		private StartStopConfig startStop;
		private DeviceType deviceType;
		private int dcFeedInThreshold;
		private int maxChargePower;
		private int maxDischargePower;
		private boolean debugMode;
		private boolean readOnlyMode;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setAlias(String alias) {
			this.alias = alias;
			return this;
		}

		public Builder setEnabled(boolean enabled) {
			this.enabled = enabled;
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

		public Builder setPhase(SingleOrAllPhase phase) {
			this.phase = phase;
			return this;
		}

		public Builder setStartStop(StartStopConfig startStop) {
			this.startStop = startStop;
			return this;
		}

		public Builder setDeviceType(DeviceType deviceType) {
			this.deviceType = deviceType;
			return this;
		}

		public Builder setDcFeedInThreshold(int dcFeedInThreshold) {
			this.dcFeedInThreshold = dcFeedInThreshold;
			return this;
		}

		public Builder setMaxChargePower(int maxChargePower) {
			this.maxChargePower = maxChargePower;
			return this;
		}

		public Builder setMaxDischargePower(int maxDischargePower) {
			this.maxDischargePower = maxDischargePower;
			return this;
		}

		public Builder setDebugMode(boolean debugMode) {
			this.debugMode = debugMode;
			return this;
		}

		public Builder setReadOnlyMode(boolean readOnlyMode) {
			this.readOnlyMode = readOnlyMode;
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
	public String id() {
		return this.builder.id;
	}

	@Override
	public String alias() {
		return this.builder.alias;
	}

	@Override
	public boolean enabled() {
		return this.builder.enabled;
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
	public SingleOrAllPhase phase() {
		return this.builder.phase;
	}

	@Override
	public StartStopConfig startStop() {
		return this.builder.startStop;
	}

	@Override
	public DeviceType DeviceType() {
		return this.builder.deviceType;
	}

	@Override
	public int dcFeedInThreshold() {
		return this.builder.dcFeedInThreshold;
	}

	@Override
	public int maxChargePower() {
		return this.builder.maxChargePower;
	}

	@Override
	public int maxDischargePower() {
		return this.builder.maxDischargePower;
	}

	@Override
	public boolean debugMode() {
		return this.builder.debugMode;
	}

	@Override
	public boolean readOnlyMode() {
		return this.builder.readOnlyMode;
	}

}
