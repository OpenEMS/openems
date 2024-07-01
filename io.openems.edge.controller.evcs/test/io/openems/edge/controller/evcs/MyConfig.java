package io.openems.edge.controller.evcs;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.edge.evcs.api.ChargeMode;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private boolean enabled = true;
		private boolean debugMode = false;
		private String evcsId = "evcs0";
		private boolean enabledCharging = true;
		private ChargeMode chargeMode = ChargeMode.FORCE_CHARGE;
		private int forceChargeMinPower = 7560;
		private int defaultChargeMinPower = 0;
		private Priority priority = Priority.CAR;
		private int energySessionLimit = 0;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
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

		public Builder setEvcsId(String evcsId) {
			this.evcsId = evcsId;
			return this;
		}

		public Builder setEnableCharging(boolean enableCharging) {
			this.enabledCharging = enableCharging;
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

	@Override
	public boolean debugMode() {
		return this.builder.debugMode;
	}

	@Override
	public String evcs_target() {
		return "(&(enabled=true)(!(service.pid=ctrlEvcs0))(|(id=" + this.evcs_id() + ")))";
	}
}
