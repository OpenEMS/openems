package io.openems.edge.timeofusetariff.entsoe;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.edge.common.currency.Currency;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String securityToken;
		private BiddingZone biddingZone;
		private Currency currency;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setSecurityToken(String securityToken) {
			this.securityToken = securityToken;
			return this;
		}

		public Builder setBididngZone(BiddingZone biddingZone) {
			this.biddingZone = biddingZone;
			return this;
		}
		
		public Builder setCurrency(Currency currency) {
			this.currency = currency;
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
	public String securityToken() {
		return this.builder.securityToken;
	}

	@Override
	public BiddingZone biddingZone() {
		return this.builder.biddingZone;
	}

	@Override
	public Currency currency() {
		return this.builder.currency;
	}

}