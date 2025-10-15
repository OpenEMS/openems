package io.openems.edge.timeofusetariff.ancillarycosts;

import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String securityToken;
		private String ancillaryCosts;
		private double fixedTariff;

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

		public Builder setAncillaryCosts(String ancillaryCosts) {
			this.ancillaryCosts = ancillaryCosts;
			return this;
		}

		public Builder setFixedTariff(double fixedTariff) {
			this.fixedTariff = fixedTariff;
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
	public String ancillaryCosts() {
		return this.builder.ancillaryCosts;
	}

	@Override
	public double fixedTariff() {
		return this.builder.fixedTariff;
	}

}