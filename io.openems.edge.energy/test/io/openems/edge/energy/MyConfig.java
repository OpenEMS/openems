package io.openems.edge.energy;

import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private boolean enabled;
		private String essId;
		private int essMaxChargePower;
		private int maxChargePowerFromGrid;
		private boolean limitChargePowerFor14aEnWG;

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

		public Builder setEssId(String essId) {
			this.essId = essId;
			return this;
		}

		public Builder setEssMaxChargePower(int essMaxChargePower) {
			this.essMaxChargePower = essMaxChargePower;
			return this;
		}

		public Builder setMaxChargePowerFromGrid(int maxChargePowerFromGrid) {
			this.maxChargePowerFromGrid = maxChargePowerFromGrid;
			return this;
		}

		public Builder setLimitChargePowerFor14aEnWG(boolean limitChargePowerFor14aEnWG) {
			this.limitChargePowerFor14aEnWG = limitChargePowerFor14aEnWG;
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
	public boolean enabled() {
		return this.builder.enabled;
	}
}