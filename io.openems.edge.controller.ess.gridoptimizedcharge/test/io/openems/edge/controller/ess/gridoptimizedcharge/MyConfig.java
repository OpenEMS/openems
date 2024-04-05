package io.openems.edge.controller.ess.gridoptimizedcharge;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.utils.ConfigUtils;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String essId;
		private String meterId;
		private int maximumSellToGridPower;
		private Mode mode;
		private String manualTargetTime;
		private boolean sellToGridLimitEnabled;
		private int sellToGridLimitRampPercentage;
		private DelayChargeRiskLevel delayChargeRiskLevel;

		public Builder setId(String id) {
			this.id = id;
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

		public Builder setMaximumSellToGridPower(int maximumSellToGridPower) {
			this.maximumSellToGridPower = maximumSellToGridPower;
			return this;
		}

		public Builder setMode(Mode mode) {
			this.mode = mode;
			return this;
		}

		public Builder setSellToGridLimitRampPercentage(int sellToGridLimitRampPercentage) {
			this.sellToGridLimitRampPercentage = sellToGridLimitRampPercentage;
			return this;
		}

		public Builder setManualTargetTime(String manualTargetTime) {
			this.manualTargetTime = manualTargetTime;
			return this;
		}

		public Builder setSellToGridLimitEnabled(boolean sellToGridLimitEnabled) {
			this.sellToGridLimitEnabled = sellToGridLimitEnabled;
			return this;
		}

		public Builder setDelayChargeRiskLevel(DelayChargeRiskLevel delayChargeRiskLevel) {
			this.delayChargeRiskLevel = delayChargeRiskLevel;
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
	public String ess_id() {
		return this.builder.essId;
	}

	@Override
	public String meter_id() {
		return this.builder.meterId;
	}

	@Override
	public int maximumSellToGridPower() {
		return this.builder.maximumSellToGridPower;
	}

	@Override
	public String ess_target() {
		return ConfigUtils.generateReferenceTargetFilter(this.id(), this.ess_id());
	}

	@Override
	public String meter_target() {
		return ConfigUtils.generateReferenceTargetFilter(this.id(), this.meter_id());
	}

	@Override
	public boolean debugMode() {
		return false;
	}

	@Override
	public Mode mode() {
		return this.builder.mode;
	}

	@Override
	public String manualTargetTime() {
		return this.builder.manualTargetTime;
	}

	@Override
	public boolean sellToGridLimitEnabled() {
		return this.builder.sellToGridLimitEnabled;
	}

	@Override
	public int sellToGridLimitRampPercentage() {
		return this.builder.sellToGridLimitRampPercentage;
	}

	@Override
	public DelayChargeRiskLevel delayChargeRiskLevel() {
		return this.builder.delayChargeRiskLevel;
	}
}
