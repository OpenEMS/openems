package io.openems.edge.timeofusetariff.manual.eeg2025.gridsell;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.types.EntsoeBiddingZone;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private double fixedGridSellPrice;
		private String securityToken;
		private EntsoeBiddingZone biddingZone;

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

		public Builder setSecurityToken(String securityToken) {
			this.securityToken = securityToken;
			return this;
		}

		public Builder setBiddingZone(EntsoeBiddingZone biddingZone) {
			this.biddingZone = biddingZone;
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
		return this.builder.securityToken;
	}

	@Override
	public EntsoeBiddingZone biddingZone() {
		return this.builder.biddingZone;
	}
}
