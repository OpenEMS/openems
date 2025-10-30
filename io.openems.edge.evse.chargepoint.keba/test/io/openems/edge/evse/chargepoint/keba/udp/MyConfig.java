package io.openems.edge.evse.chargepoint.keba.udp;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.edge.common.type.Phase.SingleOrThreePhase;
import io.openems.edge.evse.chargepoint.keba.common.enums.LogVerbosity;
import io.openems.edge.meter.api.PhaseRotation;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private boolean readOnly;
		private String ip;
		private PhaseRotation phaseRotation;
		private SingleOrThreePhase wiring;
		private boolean p30hasS10PhaseSwitching;
		private LogVerbosity logVerbosity;

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

		public Builder setIp(String ip) {
			this.ip = ip;
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

		public Builder setP30hasS10PhaseSwitching(boolean p30hasS10PhaseSwitching) {
			this.p30hasS10PhaseSwitching = p30hasS10PhaseSwitching;
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
	public String ip() {
		return this.builder.ip;
	}

	@Override
	public boolean readOnly() {
		return this.builder.readOnly;
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
	public boolean p30hasS10PhaseSwitching() {
		return this.builder.p30hasS10PhaseSwitching;
	}

	@Override
	public LogVerbosity logVerbosity() {
		return this.builder.logVerbosity;
	}
}