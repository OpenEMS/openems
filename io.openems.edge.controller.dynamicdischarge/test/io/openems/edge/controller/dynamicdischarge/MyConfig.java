package io.openems.edge.controller.dynamicdischarge;

import io.openems.edge.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String essId;
		private int predictionStartHour;
		private int maxStartHour;
		private int maxEndHour;
		private String priceConfig;

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

		public Builder setMaxStartHour(int maxStartHour) {
			this.maxStartHour = maxStartHour;
			return this;
		}

		public Builder setMaxEndHour(int maxEndHour) {
			this.maxEndHour = maxEndHour;
			return this;
		}

		public Builder setpriceConfig(String priceConfig) {
			this.priceConfig = priceConfig;
			return this;
		}

		public Builder setpredictionStartHour(int predictionStartHour) {
			this.predictionStartHour = predictionStartHour;
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
	public int maxStartHour() {
		return this.builder.maxStartHour;
	}

	@Override
	public int maxEndHour() {
		return this.builder.maxEndHour;
	}

	@Override
	public String priceConfig() {
		return this.builder.priceConfig;
	}

	@Override
	public String url() {
		return "";
	}

	@Override
	public String apikey() {
		return "";
	}

	@Override
	public int predictionStartHour() {
		return this.builder.predictionStartHour;
	}

}