package io.openems.edge.controller.ess.limittotaldischarge;

import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String essId;
		private int minSoc;
		private int forceChargeSoc;
		private int forceChargePower;

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

		public Builder setMinSoc(int minSoc) {
			this.minSoc = minSoc;
			return this;
		}

		public Builder setForceChargeSoc(int forceChargeSoc) {
			this.forceChargeSoc = forceChargeSoc;
			return this;
		}

		public Builder setForceChargePower(int forceChargePower) {
			this.forceChargePower = forceChargePower;
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
	public int minSoc() {
		return this.builder.minSoc;
	}

	@Override
	public int forceChargeSoc() {
		return this.builder.forceChargeSoc;
	}

	@Override
	public int forceChargePower() {
		return this.builder.forceChargePower;
	}

}