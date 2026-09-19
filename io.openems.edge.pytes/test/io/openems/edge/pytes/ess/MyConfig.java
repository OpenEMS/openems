package io.openems.edge.pytes.ess;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.edge.pytes.enums.EnableDisable;
import io.openems.edge.pytes.enums.RemoteDispatchRealtimeControlSwitch;
import io.openems.edge.pytes.enums.WorkMode;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String modbusId = "modbus0";
		private String meterId = "";
		private int modbusUnitId = 1;
		private WorkMode workMode = WorkMode.EXTERNAL;
		private RemoteDispatchRealtimeControlSwitch essSetpoint = RemoteDispatchRealtimeControlSwitch.BATTERY_CONTROL;
		private int maxApparentPower = 10000;
		private int minSoc = 10;
		private boolean enableBackupPort = true;
		private boolean debugMode = false;
		private boolean extendedDebugMode = false;
		private boolean readOnlyMode = false;

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

		public Builder setMeterId(String meterId) {
			this.meterId = meterId;
			return this;
		}

		public Builder setModbusUnitId(int modbusUnitId) {
			this.modbusUnitId = modbusUnitId;
			return this;
		}

		public Builder setWorkMode(WorkMode workMode) {
			this.workMode = workMode;
			return this;
		}

		public Builder setEssSetpoint(RemoteDispatchRealtimeControlSwitch essSetpoint) {
			this.essSetpoint = essSetpoint;
			return this;
		}

		public Builder setMaxApparentPower(int maxApparentPower) {
			this.maxApparentPower = maxApparentPower;
			return this;
		}

		public Builder setMinSoc(int minSoc) {
			this.minSoc = minSoc;
			return this;
		}

		public Builder setEnableBackupPort(boolean enableBackupPort) {
			this.enableBackupPort = enableBackupPort;
			return this;
		}

		public Builder setDebugMode(boolean debugMode) {
			this.debugMode = debugMode;
			return this;
		}

		public Builder setExtendedDebugMode(boolean extendedDebugMode) {
			this.extendedDebugMode = extendedDebugMode;
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
	public String modbus_id() {
		return this.builder.modbusId;
	}

	@Override
	public int modbusUnitId() {
		return this.builder.modbusUnitId;
	}

	@Override
	public WorkMode workMode() {
		return this.builder.workMode;
	}

	@Override
	public RemoteDispatchRealtimeControlSwitch essSetpoint() {
		return this.builder.essSetpoint;
	}

	@Override
	public int maxApparentPower() {
		return this.builder.maxApparentPower;
	}

	@Override
	public int minSoc() {
		return this.builder.minSoc;
	}

	@Override
	public boolean enableBackupPort() {
		return this.builder.enableBackupPort;
	}

	@Override
	public EnableDisable feedPowerEnable() {
		return EnableDisable.DISABLE;
	}

	@Override
	public int failsafeMinutes() {
		return 5;
	}

	@Override
	public String meter_id() {
		return this.builder.meterId;
	}

	@Override
	public boolean debugMode() {
		return this.builder.debugMode;
	}

	@Override
	public boolean extendedDebugMode() {
		return this.builder.extendedDebugMode;
	}

	@Override
	public boolean readOnlyMode() {
		return this.builder.readOnlyMode;
	}

}
