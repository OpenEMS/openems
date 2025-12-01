package io.openems.edge.controller.evse.single;

import static io.openems.common.utils.ConfigUtils.generateReferenceTargetFilter;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.edge.evse.api.chargepoint.Mode;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	public static class Builder {
		private String id;
		private String chargePointId;
		private Mode mode;
		private String electricVehicleId;
		private PhaseSwitching phaseSwitching;
		private String smartConfig;
		private int manualEnergySessionLimit;
		private LogVerbosity logVerbosity;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setChargePointId(String chargePointId) {
			this.chargePointId = chargePointId;
			return this;
		}

		public Builder setMode(Mode mode) {
			this.mode = mode;
			return this;
		}

		public Builder setPhaseSwitching(PhaseSwitching phaseSwitching) {
			this.phaseSwitching = phaseSwitching;
			return this;
		}

		public Builder setElectricVehicleId(String electricVehicleId) {
			this.electricVehicleId = electricVehicleId;
			return this;
		}

		public Builder setSmartConfig(String smartConfig) {
			this.smartConfig = smartConfig;
			return this;
		}

		public Builder setManualEnergySessionLimit(int manualEnergySessionLimit) {
			this.manualEnergySessionLimit = manualEnergySessionLimit;
			return this;
		}

		public Builder setLogVerbosity(LogVerbosity logVerbosity) {
			this.logVerbosity = logVerbosity;
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
	public String chargePoint_id() {
		return this.builder.chargePointId;
	}

	@Override
	public Mode mode() {
		return this.builder.mode;
	}

	@Override
	public String electricVehicle_id() {
		return this.builder.electricVehicleId;
	}

	@Override
	public PhaseSwitching phaseSwitching() {
		return this.builder.phaseSwitching;
	}

	@Override
	public String smartConfig() {
		return this.builder.smartConfig;
	}

	@Override
	public int manualEnergySessionLimit() {
		return this.builder.manualEnergySessionLimit;
	}

	@Override
	public LogVerbosity logVerbosity() {
		return this.builder.logVerbosity;
	}

	@Override
	public String chargePoint_target() {
		return generateReferenceTargetFilter(this.id(), this.chargePoint_id());
	}

	@Override
	public String electricVehicle_target() {
		return generateReferenceTargetFilter(this.id(), this.electricVehicle_id());
	}
}
