package io.openems.edge.evcs.cluster;

import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfigPeakShaving extends AbstractComponentConfig implements ConfigPeakShaving {

	protected static class Builder {

		private String id = "evcsCluster0";
		private boolean debugMode = false;
		private int hardwarePowerLimitPerPhase = 7000;
		private String[] evcsIds = { "evcs0", "evcs1" };
		private String evcsTarget = "(&(enabled=true)(!(service.pid=evcsCluster0))(|(id=\" + this.evcs_id() + \")))";
		private String essId = "ess0";
		private String meterId = "meter0";
		private int essSecureDischargeSoc = 25;
		private int essSecureDischargeMinSoc = 15;
		private boolean enableSecureEssDischarge = false;

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

		public Builder setEvcsTarget(String evcsTarget) {
			this.evcsTarget = evcsTarget;
			return this;
		}

		public Builder setEssId(String essId) {
			this.essId = essId;
			return this;
		}

		public Builder setEssSecureDischargeSoc(int essSecureDischargeSoc) {
			this.essSecureDischargeSoc = essSecureDischargeSoc;
			return this;
		}

		public Builder setEssSecureDischargeMinSoc(int essSecureDischargeMinSoc) {
			this.essSecureDischargeMinSoc = essSecureDischargeMinSoc;
			return this;
		}

		public Builder setEnableSecureEssDischarge(boolean enableSecureEssDischarge) {
			this.enableSecureEssDischarge = enableSecureEssDischarge;
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

	/**
	 * Create a Config builder.
	 * 
	 * @return a {@link Builder}
	 */
	public static Builder create() {
		return new Builder();
	}

	private final Builder builder;

	private MyConfigPeakShaving(Builder builder) {
		super(ConfigPeakShaving.class, builder.id);
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
		return this.builder.evcsTarget;
	}

	@Override
	public int hardwarePowerLimitPerPhase() {
		return this.builder.hardwarePowerLimitPerPhase;
	}

	@Override
	public String meter_id() {
		return this.builder.meterId;
	}

	@Override
	public boolean enable_secure_ess_discharge() {
		return this.builder.enableSecureEssDischarge;
	}

	@Override
	public int ess_secure_discharge_soc() {
		return this.builder.essSecureDischargeSoc;
	}

	@Override
	public int ess_secure_discharge_min_soc() {
		return this.builder.essSecureDischargeMinSoc;
	}
}
