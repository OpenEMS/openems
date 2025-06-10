package io.openems.edge.controller.asymmetric.balancingcosphi;

import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private CosPhiDirection direction;
		private double cosPhi;
		private String meterId;
		private String essId;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setDirection(CosPhiDirection direction) {
			this.direction = direction;
			return this;
		}

		public Builder setCosPhi(double cosPhi) {
			this.cosPhi = cosPhi;
			return this;
		}

		public Builder setMeterId(String meterId) {
			this.meterId = meterId;
			return this;
		}

		public Builder setEssId(String essId) {
			this.essId = essId;
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
	public String meter_id() {
		return this.builder.meterId;
	}

	@Override
	public double cosPhi() {
		return this.builder.cosPhi;
	}

	@Override
	public CosPhiDirection direction() {
		return this.builder.direction;
	}
}