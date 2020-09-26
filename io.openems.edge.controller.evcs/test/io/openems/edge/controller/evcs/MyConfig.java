package io.openems.edge.controller.evcs;

import io.openems.common.utils.ConfigUtils;
import io.openems.edge.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id = null;
		private String essId = null;
		public boolean debugMode;
		public String evcsId;
		public boolean enabledCharging;
		public ChargeMode chargeMode;
		public int forceChargeMinPower;
		public int defaultChargeMinPower;
		public Priority priority;
		public int energySessionLimit;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setEssId(String essId) {
			this.essId = essId;
			return this;
		}

		public Builder setDebugMode(boolean debugMode) {
			this.debugMode = debugMode;
			return this;
		}

		public Builder setEvcsId(String evcsId) {
			this.evcsId = evcsId;
			return this;
		}

		public Builder setEnabledCharging(boolean enabledCharging) {
			this.enabledCharging = enabledCharging;
			return this;
		}

		public Builder setChargeMode(ChargeMode chargeMode) {
			this.chargeMode = chargeMode;
			return this;
		}

		public Builder setForceChargeMinPower(int forceChargeMinPower) {
			this.forceChargeMinPower = forceChargeMinPower;
			return this;
		}

		public Builder setDefaultChargeMinPower(int defaultChargeMinPower) {
			this.defaultChargeMinPower = defaultChargeMinPower;
			return this;
		}

		public Builder setPriority(Priority priority) {
			this.priority = priority;
			return this;
		}

		public Builder setEnergySessionLimit(int energySessionLimit) {
			this.energySessionLimit = energySessionLimit;
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
	public String ess_id() {
		return this.builder.essId;
	}

	@Override
	public boolean debugMode() {
		return this.builder.debugMode;
	}

	@Override
	public String evcs_id() {
		return this.builder.evcsId;
	}

	@Override
	public boolean enabledCharging() {
		return this.builder.enabledCharging;
	}

	@Override
	public ChargeMode chargeMode() {
		return this.builder.chargeMode;
	}

	@Override
	public int forceChargeMinPower() {
		return this.builder.forceChargeMinPower;
	}

	@Override
	public int defaultChargeMinPower() {
		return this.builder.defaultChargeMinPower;
	}

	@Override
	public Priority priority() {
		return this.builder.priority;
	}

	@Override
	public int energySessionLimit() {
		return this.builder.energySessionLimit;
	}

}