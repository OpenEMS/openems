package io.openems.edge.timeofusetariff.manual.eeg2025.gridsell;

import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private double fixedGridSellPrice;
		private int serviceRanking = 20;

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

		public Builder setServiceRanking(int serviceRanking) {
			this.serviceRanking = serviceRanking;
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

	@Override
	public String securityToken() {
		return "";
	}

	@Override
	public int service_ranking() {
		return this.builder.serviceRanking;
	}
}
