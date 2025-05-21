package io.openems.edge.controller.symmetric.randompower;

import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id = null;
		private String essId;
		private int minPower;
		private int maxPower;

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

		public Builder setMinPower(int minPower) {
			this.minPower = minPower;
			return this;
		}

		public Builder setMaxPower(int maxPower) {
			this.maxPower = maxPower;
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
	public int minPower() {
		return this.builder.minPower;
	}

	@Override
	public int maxPower() {
		return this.builder.maxPower;
	}

}