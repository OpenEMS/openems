package io.openems.edge.simulator.evse.chargepoint;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.edge.common.type.Phase.SingleOrThreePhase;
import io.openems.edge.meter.api.PhaseRotation;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id = "evseChargePoint0";
		private boolean readOnly = false;
		private boolean vehicleConnected = true;
		private int maxCurrent = 16000;
		private int minCurrent = 6000;
		private int voltage = 230;
		private SingleOrThreePhase wiring = SingleOrThreePhase.THREE_PHASE;
		private boolean supportsPhaseSwitching = false;
		private PhaseRotation phaseRotation = PhaseRotation.L1_L2_L3;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setReadOnly(boolean readOnly) {
			this.readOnly = readOnly;
			return this;
		}

		public Builder setVehicleConnected(boolean vehicleConnected) {
			this.vehicleConnected = vehicleConnected;
			return this;
		}

		public Builder setMaxCurrent(int maxCurrent) {
			this.maxCurrent = maxCurrent;
			return this;
		}

		public Builder setMinCurrent(int minCurrent) {
			this.minCurrent = minCurrent;
			return this;
		}

		public Builder setVoltage(int voltage) {
			this.voltage = voltage;
			return this;
		}

		public Builder setWiring(SingleOrThreePhase wiring) {
			this.wiring = wiring;
			return this;
		}

		public Builder setSupportsPhaseSwitching(boolean supportsPhaseSwitching) {
			this.supportsPhaseSwitching = supportsPhaseSwitching;
			return this;
		}

		public Builder setPhaseRotation(PhaseRotation phaseRotation) {
			this.phaseRotation = phaseRotation;
			return this;
		}

		public MyConfig build() {
			return new MyConfig(this);
		}
	}

	/**
	 * Create a Config builder.
	 *
	 * @return a {@link MyConfig.Builder}
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
	public boolean readOnly() {
		return this.builder.readOnly;
	}

	@Override
	public boolean vehicleConnected() {
		return this.builder.vehicleConnected;
	}

	@Override
	public int maxCurrent() {
		return this.builder.maxCurrent;
	}

	@Override
	public int minCurrent() {
		return this.builder.minCurrent;
	}

	@Override
	public int voltage() {
		return this.builder.voltage;
	}

	@Override
	public SingleOrThreePhase wiring() {
		return this.builder.wiring;
	}

	@Override
	public boolean supportsPhaseSwitching() {
		return this.builder.supportsPhaseSwitching;
	}

	@Override
	public PhaseRotation phaseRotation() {
		return this.builder.phaseRotation;
	}
}
