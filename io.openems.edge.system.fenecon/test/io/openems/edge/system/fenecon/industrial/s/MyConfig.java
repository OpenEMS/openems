package io.openems.edge.system.fenecon.industrial.s;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.utils.ConfigUtils;
import io.openems.edge.common.startstop.StartStopConfig;
import io.openems.edge.system.fenecon.industrial.s.enums.CoolingUnitMode;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private StartStopConfig startStopConfig;
		private CoolingUnitMode coolingUnitMode;
		private String essId;
		private String[] batteryIds;
		private String coolingUnitError;
		private String coolingUnitEnable;
		private String emergencyStopState;
		private String acknowledgeEmergencyStop;
		private String spdState;
		private String fuseState;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setStartStopConfig(StartStopConfig startStopConfig) {
			this.startStopConfig = startStopConfig;
			return this;
		}

		public Builder setCoolingUnitMode(CoolingUnitMode coolingUnitMode) {
			this.coolingUnitMode = coolingUnitMode;
			return this;
		}

		public Builder setEssId(String essId) {
			this.essId = essId;
			return this;
		}

		public Builder setBatteryIds(String... batteryIds) {
			this.batteryIds = batteryIds;
			return this;
		}

		public Builder setCoolingUnitError(String coolingUnitError) {
			this.coolingUnitError = coolingUnitError;
			return this;
		}

		public Builder setCoolingUnitEnable(String coolingUnitEnable) {
			this.coolingUnitEnable = coolingUnitEnable;
			return this;
		}

		public Builder setEmergencyStopState(String emergencyStopState) {
			this.emergencyStopState = emergencyStopState;
			return this;
		}

		public Builder setAcknowledgeEmergencyStop(String acknowledgeEmergencyStop) {
			this.acknowledgeEmergencyStop = acknowledgeEmergencyStop;
			return this;
		}

		public Builder setSpdState(String spdState) {
			this.spdState = spdState;
			return this;
		}

		public Builder setFuseState(String fuseState) {
			this.fuseState = fuseState;
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
	public StartStopConfig startStop() {
		return this.builder.startStopConfig;
	}

	@Override
	public CoolingUnitMode coolingUnitMode() {
		return this.builder.coolingUnitMode;
	}

	@Override
	public String ess_id() {
		return this.builder.essId;
	}

	@Override
	public String[] battery_ids() {
		return this.builder.batteryIds;
	}

	@Override
	public String coolingUnitError() {
		return this.builder.coolingUnitError;
	}

	@Override
	public String coolingUnitEnable() {
		return this.builder.coolingUnitEnable;
	}

	@Override
	public String emergencyStopState() {
		return this.builder.emergencyStopState;
	}

	@Override
	public String acknowledgeEmergencyStop() {
		return this.builder.acknowledgeEmergencyStop;
	}

	@Override
	public String spdTripped() {
		return this.builder.spdState;
	}

	@Override
	public String fuseTripped() {
		return this.builder.fuseState;
	}

	@Override
	public String ess_target() {
		return ConfigUtils.generateReferenceTargetFilter(this.id(), this.ess_id());
	}

	@Override
	public String Battery_target() {
		return ConfigUtils.generateReferenceTargetFilter(this.id(), this.battery_ids());
	}
}