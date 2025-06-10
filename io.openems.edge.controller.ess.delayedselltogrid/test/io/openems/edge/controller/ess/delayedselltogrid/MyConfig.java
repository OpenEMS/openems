package io.openems.edge.controller.ess.delayedselltogrid;

import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String essId;
		private String meterId;
		private int sellToGridPowerLimit;
		private int continuousSellToGridPower;

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

		public Builder setContinuousSellToGridPower(int continuousSellToGridPower) {
			this.continuousSellToGridPower = continuousSellToGridPower;
			return this;
		}

		public Builder setSellToGridPowerLimit(int sellToGridPowerLimit) {
			this.sellToGridPowerLimit = sellToGridPowerLimit;
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
	public int sellToGridPowerLimit() {
		return this.builder.sellToGridPowerLimit;
	}

	@Override
	public int continuousSellToGridPower() {
		return this.builder.continuousSellToGridPower;
	}

	@Override
	public String meter_id() {
		return this.builder.meterId;
	}
}