package io.openems.edge.evcs.cluster;

import io.openems.common.utils.ConfigUtils;
import io.openems.edge.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfigPeakShaving extends AbstractComponentConfig implements ConfigPeakShaving {

	protected static class Builder {
		private String id;
		private String meterId;
		private String essId;
		private String[] evcsIds;
		private int hardwarePowerLimitPerPhase;
		private boolean debugMode;

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

		public Builder setEssId(String essId) {
			this.essId = essId;
			return this;
		}

		public Builder setHardwarePowerLimitPerPhase(int hardwarePowerLimitPerPhase) {
			this.hardwarePowerLimitPerPhase = hardwarePowerLimitPerPhase;
			return this;
		}

		public Builder setEvcsIds(String... evcsIds) {
			this.evcsIds = evcsIds;
			return this;
		}

		public Builder setMeterId(String meterId) {
			this.meterId = meterId;
			return this;
		}

		public MyConfigPeakShaving build() {
			return new MyConfigPeakShaving(this);
		}
	}

	public static Builder create() {
		return new Builder();
	}

	private final Builder builder;

	private MyConfigPeakShaving(Builder builder) {
		super(ConfigPeakShaving.class, builder.id);
		this.builder = builder;
	}

	@Override
	public boolean debugMode() {
		return this.builder.debugMode;
	}

	@Override
	public int hardwarePowerLimitPerPhase() {
		return this.builder.hardwarePowerLimitPerPhase;
	}

	@Override
	public String[] evcs_ids() {
		return this.builder.evcsIds;
	}

	@Override
	public String Evcs_target() {
		return ConfigUtils.generateReferenceTargetFilter(this.id(), this.evcs_ids());
	}

	@Override
	public String ess_id() {
		return this.builder.essId;
	}

	@Override
	public String meter_id() {
		return this.builder.meterId;
	}

}
