package io.openems.edge.evse.chargepoint.hardybarth.ecb1;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.edge.meter.api.PhaseRotation;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String ip;
		private int chargeControlId;
		private int meterId;
		private int minHwCurrent;
		private int maxHwCurrent;
		private PhaseRotation phaseRotation = PhaseRotation.L1_L2_L3;
		private boolean readOnly = false;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setIp(String ip) {
			this.ip = ip;
			return this;
		}

		public Builder setChargeControlId(int chargeControlId) {
			this.chargeControlId = chargeControlId;
			return this;
		}

		public Builder setMeterId(int meterId) {
			this.meterId = meterId;
			return this;
		}

		public Builder setMinHwCurrent(int minHwCurrent) {
			this.minHwCurrent = minHwCurrent;
			return this;
		}

		public Builder setMaxHwCurrent(int maxHwCurrent) {
			this.maxHwCurrent = maxHwCurrent;
			return this;
		}

		public Builder setPhaseRotation(PhaseRotation phaseRotation) {
			this.phaseRotation = phaseRotation;
			return this;
		}

		public Builder setReadOnly(boolean readOnly) {
			this.readOnly = readOnly;
			return this;
		}

		public MyConfig build() {
			return new MyConfig(this);
		}
	}

	public static Builder create() {
		return new Builder();
	}

	private final Builder builder;

	private MyConfig(Builder builder) {
		super(Config.class, builder.id);
		this.builder = builder;
	}

	@Override
	public String ip() {
		return this.builder.ip;
	}

	@Override
	public int chargeControlId() {
		return this.builder.chargeControlId;
	}

	@Override
	public int meterId() {
		return this.builder.meterId;
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
}
