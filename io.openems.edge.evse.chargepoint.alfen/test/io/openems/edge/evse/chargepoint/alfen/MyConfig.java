package io.openems.edge.evse.chargepoint.alfen;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.edge.common.type.Phase.SingleOrThreePhase;
import io.openems.edge.meter.api.PhaseRotation;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private boolean readOnly;
		private String modbusId;
		private PhaseRotation phaseRotation = PhaseRotation.L1_L2_L3;
		private SingleOrThreePhase wiring = SingleOrThreePhase.THREE_PHASE;
		private int modbusUnitId = 1;

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

		public Builder setModbusId(String modbusId) {
			this.modbusId = modbusId;
			return this;
		}

		public Builder setPhaseRotation(PhaseRotation phaseRotation) {
			this.phaseRotation = phaseRotation;
			return this;
		}

		public Builder setWiring(SingleOrThreePhase wiring) {
			this.wiring = wiring;
			return this;
		}

		public Builder setModbusUnitId(int modbusUnitId) {
			this.modbusUnitId = modbusUnitId;
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
	public String modbus_id() {
		return this.builder.modbusId;
	}

	@Override
	public PhaseRotation phaseRotation() {
		return this.builder.phaseRotation;
	}

	@Override
	public SingleOrThreePhase wiring() {
		return this.builder.wiring;
	}

	@Override
	public int modbusUnitId() {
		return this.builder.modbusUnitId;
	}
}
