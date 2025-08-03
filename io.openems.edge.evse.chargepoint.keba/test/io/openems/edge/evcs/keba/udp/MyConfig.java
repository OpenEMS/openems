package io.openems.edge.evcs.keba.udp;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.edge.evcs.api.PhaseRotation;
import io.openems.edge.evse.chargepoint.keba.common.enums.LogVerbosity;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private boolean readOnly;
		private String ip;
		private boolean debugMode;
		private int minHwCurrent;
		private PhaseRotation phaseRotation;
		private boolean useDisplay;
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

		public Builder setMinHwCurrent(int minHwCurrent) {
			this.minHwCurrent = minHwCurrent;
			return this;
		}

		public Builder setIp(String ip) {
			this.ip = ip;
			return this;
		}

		public Builder setDebugMode(boolean debugMode) {
			this.debugMode = debugMode;
			return this;
		}

		public Builder setPhaseRotation(PhaseRotation phaseRotation) {
			this.phaseRotation = phaseRotation;
			return this;
		}

		public Builder setUseDisplay(boolean useDisplay) {
			this.useDisplay = useDisplay;
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
	public boolean readOnly() {
		return this.builder.readOnly;
	}

	@Override
	public String ip() {
		return this.builder.ip;
	}

	@Override
	public int minHwCurrent() {
		return this.builder.minHwCurrent;
	}

	@Override
	public PhaseRotation phaseRotation() {
		return this.builder.phaseRotation;
	}

	@Override
	public boolean useDisplay() {
		return this.builder.useDisplay;
	}

	@Override
	public LogVerbosity logVerbosity() {
		return this.builder.logVerbosity;
	}
}