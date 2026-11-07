package io.openems.edge.timeofusetariff.spottariff.gridsell;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.types.BiddingZone;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private BiddingZone biddingZone;
		private String calculateExpression = "";
		private int serviceRanking = 10;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setBiddingZone(BiddingZone biddingZone) {
			this.biddingZone = biddingZone;
			return this;
		}

		public Builder setCalculateExpression(String calculateExpression) {
			this.calculateExpression = calculateExpression;
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
	public BiddingZone biddingZone() {
		return this.builder.biddingZone;
	}

	@Override
	public String calculateExpression() {
		return this.builder.calculateExpression;
	}

	@Override
	public int service_ranking() {
		return this.builder.serviceRanking;
	}

}