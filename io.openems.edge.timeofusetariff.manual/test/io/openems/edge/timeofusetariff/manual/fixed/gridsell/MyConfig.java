package io.openems.edge.timeofusetariff.manual.fixed.gridsell;

import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private double fixedGridSellPrice;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setFixedGridSellPrice(double fixedGridSellPrice) {
			this.fixedGridSellPrice = fixedGridSellPrice;
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
	public double fixedGridSellPrice() {
		return this.builder.fixedGridSellPrice;
	}
}