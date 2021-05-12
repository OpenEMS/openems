package io.openems.edge.controller.ess.gridoptimizedcharge;

import io.openems.edge.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		public String essId;
		public String meterId;
		public int maximumSellToGridPower;
		public int noOfBufferMinutes;
		public Mode mode;
		public String manual_targetTime;
		public boolean sellToGridLimitEnabled;
		public int sellToGridLimitRampPercentage;

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

		public Builder setMeterId(String meterId) {
			this.meterId = meterId;
			return this;
		}

		public Builder setMaximumSellToGridPower(int maximumSellToGridPower) {
			this.maximumSellToGridPower = maximumSellToGridPower;
			return this;
		}

		public Builder setNoOfBufferMinutes(int noOfBufferMinutes) {
			this.noOfBufferMinutes = noOfBufferMinutes;
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

		public Builder setManual_targetTime(String manual_targetTime) {
			this.manual_targetTime = manual_targetTime;
			return this;
		}

		public Builder setSellToGridLimitEnabled(boolean sellToGridLimitEnabled) {
			this.sellToGridLimitEnabled = sellToGridLimitEnabled;
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
	public int noOfBufferMinutes() {
		return this.builder.noOfBufferMinutes;
	}

	@Override
	public String ess_target() {
		return "(&(enabled=true)(!(service.pid=" + this.builder.id + "))(|(id=" + this.ess_id() + ")))";
	}

	@Override
	public String meter_target() {
		return "(&(enabled=true)(!(service.pid=" + this.builder.id + "))(|(id=" + this.meter_id() + ")))";
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
	public String manual_targetTime() {
		return this.builder.manual_targetTime;
	}

	@Override
	public boolean sellToGridLimitEnabled() {
		return this.builder.sellToGridLimitEnabled;
	}

	@Override
	public int sellToGridLimitRampPercentage() {
		return this.builder.sellToGridLimitRampPercentage;
	}
}