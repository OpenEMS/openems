package io.openems.edge.evcs.cluster;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.utils.ConfigUtils;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {

		private String id = "evcsCluster0";
		private boolean debugMode = false;
		private int hardwarePowerLimitPerPhase = 7000;
		private String[] evcsIds = { "evcs0", "evcs1" };
		private String essId = "ess0";
		private String meterId = "meter0";

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setDebugMode(boolean debugMode) {
			this.debugMode = debugMode;
			return this;
		}

		public Builder setHardwarePowerLimit(int hardwarePowerLimitPerPhase) {
			this.hardwarePowerLimitPerPhase = hardwarePowerLimitPerPhase;
			return this;
		}

		public Builder setEvcsIds(String[] evcsIds) {
			this.evcsIds = evcsIds;
			return this;
		}

		public Builder setEssId(String essId) {
			this.essId = essId;
			return this;
		}

		public Builder setMeterId(String meterId) {
			this.meterId = meterId;
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
	public boolean debugMode() {
		return this.builder.debugMode;
	}

	@Override
	public String[] evcs_ids() {
		return this.builder.evcsIds;
	}

	@Override
	public String ess_id() {
		return this.builder.essId;
	}

	@Override
	public String Evcs_target() {
		return ConfigUtils.generateReferenceTargetFilter(this.id(), this.evcs_ids());
	}

	@Override
	public int hardwarePowerLimitPerPhase() {
		return this.builder.hardwarePowerLimitPerPhase;
	}

	@Override
	public String meter_id() {
		return this.builder.meterId;
	}
}
