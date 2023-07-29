package io.openems.edge.controller.asymmetric.fixreactivepower;

import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id = null;
		private String essId;
		private int powerL1;
		private int powerL2;
		private int powerL3;

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

		public Builder setPowerL1(int powerL1) {
			this.powerL1 = powerL1;
			return this;
		}

		public Builder setPowerL2(int powerL2) {
			this.powerL2 = powerL2;
			return this;
		}

		public Builder setPowerL3(int powerL3) {
			this.powerL3 = powerL3;
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
	public int powerL1() {
		return this.builder.powerL1;
	}

	@Override
	public int powerL2() {
		return this.builder.powerL2;
	}

	@Override
	public int powerL3() {
		return this.builder.powerL3;
	}
}