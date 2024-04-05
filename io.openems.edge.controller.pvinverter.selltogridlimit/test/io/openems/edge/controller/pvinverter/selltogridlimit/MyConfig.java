package io.openems.edge.controller.pvinverter.selltogridlimit;

import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String meterId;
		private String pvInverterId;
		private boolean asymmetricMode;
		private int maximumSellToGridPower;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setMeterId(String meterId) {
			this.meterId = meterId;
			return this;
		}

		public Builder setPvInverterId(String pvInverterId) {
			this.pvInverterId = pvInverterId;
			return this;
		}

		public Builder setAsymmetricMode(boolean asymmetricMode) {
			this.asymmetricMode = asymmetricMode;
			return this;
		}

		public Builder setMaximumSellToGridPower(int maximumSellToGridPower) {
			this.maximumSellToGridPower = maximumSellToGridPower;
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
	public String pvInverter_id() {
		return this.builder.pvInverterId;
	}

	@Override
	public String meter_id() {
		return this.builder.meterId;
	}

	@Override
	public boolean asymmetricMode() {
		return this.builder.asymmetricMode;
	}

	@Override
	public int maximumSellToGridPower() {
		return this.builder.maximumSellToGridPower;
	}
}
