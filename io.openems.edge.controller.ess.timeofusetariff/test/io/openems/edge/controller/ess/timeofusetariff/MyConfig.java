package io.openems.edge.controller.ess.timeofusetariff;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.utils.ConfigUtils;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private boolean enabled;
		private String essId;
		private Mode mode;
		private ControlMode controlMode;
		private int essMaxChargePower;
		private int maxChargePowerFromGrid;
		private boolean limitChargePowerFor14aEnWG;
		private RiskLevel riskLevel;

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

		public Builder setMode(Mode mode) {
			this.mode = mode;
			return this;
		}

		public Builder setControlMode(ControlMode controlMode) {
			this.controlMode = controlMode;
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

		public Builder setRiskLevel(RiskLevel riskLevel) {
			this.riskLevel = riskLevel;
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

	@Override
	public String ess_id() {
		return this.builder.essId;
	}

	@Override
	public Mode mode() {
		return this.builder.mode;
	}

	@Override
	public ControlMode controlMode() {
		return this.builder.controlMode;
	}

	@Override
	public int maxChargePowerFromGrid() {
		return this.builder.maxChargePowerFromGrid;
	}

	@Override
	public boolean limitChargePowerFor14aEnWG() {
		return this.builder.limitChargePowerFor14aEnWG;
	}

	@Override
	public RiskLevel riskLevel() {
		return this.builder.riskLevel;
	}

	@Override
	public String ess_target() {
		return ConfigUtils.generateReferenceTargetFilter(this.id(), this.ess_id());
	}

}