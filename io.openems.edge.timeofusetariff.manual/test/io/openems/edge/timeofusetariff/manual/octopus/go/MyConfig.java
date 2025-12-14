package io.openems.edge.timeofusetariff.manual.octopus.go;

import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private double standardPrice;
		private double lowPrice;
		private String ancillaryCosts;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setStandardPrice(double standardPrice) {
			this.standardPrice = standardPrice;
			return this;
		}

		public Builder setLowPrice(double lowPrice) {
			this.lowPrice = lowPrice;
			return this;
		}

		public Builder setAncillaryCosts(String ancillaryCosts) {
			this.ancillaryCosts = ancillaryCosts;
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
	public double standardPrice() {
		return this.builder.standardPrice;
	}

	@Override
	public double lowPrice() {
		return this.builder.lowPrice;
	}

	@Override
	public String ancillaryCosts() {
		return this.builder.ancillaryCosts;
	}
}