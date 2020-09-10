package io.openems.edge.evcs.cluster;

import java.lang.annotation.Annotation;
import java.util.Arrays;

import io.openems.edge.common.startstop.StartStopConfig;
import io.openems.edge.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfigPeakShaving extends AbstractComponentConfig implements ConfigPeakShaving {

	protected static class Builder {

		private String id = "evcsCluster0";
		private String alias = "Evcs Cluster";
		private boolean enabled = true;
		private boolean debugMode  = false;
		private int hardwarePowerLimitPerPhase = 7000;
		private String[] evcs_ids = {"evcs0", "evcs1"};
		private String evcsTarget = "(&(enabled=true)(!(service.pid=evcsCluster0))(|(id=\" + this.evcs_id() + \")))";
		private String ess_id = "ess0";
		private String meter_id = "meter0";

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setAlias(String alias) {
			this.alias = alias;
			return this;
		}

		public Builder setEnabled(boolean enabled) {
			this.enabled = enabled;
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
}
