package io.openems.edge.evse.chargepoint.keba;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.edge.evse.api.SingleThreePhase;
import io.openems.edge.evse.api.chargepoint.PhaseRotation;
import io.openems.edge.evse.chargepoint.keba.enums.P30S10PhaseSwitching;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private boolean readOnly;
		private boolean debugMode;
		private String modbusId;
		private PhaseRotation phaseRotation;
		private SingleThreePhase wiring;
		private P30S10PhaseSwitching p30S10PhaseSwitching;

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

		public Builder setDebugMode(boolean debugMode) {
			this.debugMode = debugMode;
			return this;
		}

		public Builder setModbusId(String modbusId) {
			this.modbusId = modbusId;
			return this;
		}

		public Builder setPhaseRotation(PhaseRotation phaseRotation) {
			this.phaseRotation = phaseRotation;
			return this;
		}

		public Builder setWiring(SingleThreePhase wiring) {
			this.wiring = wiring;
			return this;
		}

		public Builder setP30S10PhaseSwitching(P30S10PhaseSwitching p30s10PhaseSwitching) {
			this.p30S10PhaseSwitching = p30s10PhaseSwitching;
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
	public boolean readOnly() {
		return this.builder.readOnly;
	}

	@Override
	public boolean debugMode() {
		return this.builder.debugMode;
	}

	@Override
	public String modbus_id() {
		return this.builder.modbusId;
	}

	@Override
	public PhaseRotation phaseRotation() {
		return this.builder.phaseRotation;
	}

	@Override
	public SingleThreePhase wiring() {
		return this.builder.wiring;
	}

	@Override
	public P30S10PhaseSwitching p30S10PhaseSwitching() {
		return this.builder.p30S10PhaseSwitching;
	}
}