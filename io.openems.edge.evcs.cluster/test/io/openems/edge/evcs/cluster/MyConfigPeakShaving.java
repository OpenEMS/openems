package io.openems.edge.evcs.cluster;

import io.openems.edge.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfigPeakShaving extends AbstractComponentConfig implements ConfigPeakShaving {

	protected static class Builder {

		private String id = "evcsCluster0";
		private boolean debugMode = false;
		private int hardwarePowerLimitPerPhase = 7000;
		private String[] evcs_ids = { "evcs0", "evcs1" };
		private String evcsTarget = "(&(enabled=true)(!(service.pid=evcsCluster0))(|(id=\" + this.evcs_id() + \")))";
		private String ess_id = "ess0";
		private String meter_id = "meter0";
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

		public Builder setEvcsIds(String[] evcs_ids) {
			this.evcs_ids = evcs_ids;
			return this;
		}

		public Builder setEvcsTarget(String evcsTarget) {
			this.evcsTarget = evcsTarget;
			return this;
		}

		public Builder setEssId(String ess_id) {
			this.ess_id = ess_id;
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

		public Builder setMeterId(String meter_id) {
			this.meter_id = meter_id;
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
	public String id() {
		return this.builder.id;
	}

	@Override
	public boolean debugMode() {
		return this.builder.debugMode;
	}

	@Override
	public String[] evcs_ids() {
		return this.builder.evcs_ids;
	}

	@Override
	public String ess_id() {
		return this.builder.ess_id;
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
		return this.builder.meter_id;
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
