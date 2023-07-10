package io.openems.edge.controller.pvinverter.fixpowerlimit;

import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id = null;
		private String pvInverterId;
		private int powerLimit;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setPvInverterId(String pvInverterId) {
			this.pvInverterId = pvInverterId;
			return this;
		}

		public Builder setPowerLimit(int powerLimit) {
			this.powerLimit = powerLimit;
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
	public int powerLimit() {
		return this.builder.powerLimit;
	}
}