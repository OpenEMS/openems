package io.openems.edge.controller.ess.limittotalcharge;

import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String essId;
		private HybridEssMode hybridEssMode = HybridEssMode.TARGET_AC;

		private int maxSoc;
		private int forceDischargeSoc;
		private int forceDischargePower;

		private Builder() {
		}

		public Builder setHybridEssMode(HybridEssMode mode) {
			this.hybridEssMode = mode;
			return this;
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setEssId(String essId) {
			this.essId = essId;
			return this;
		}

		public Builder setMaxSoc(int maxSoc) {
			this.maxSoc = maxSoc;
			return this;
		}

		public Builder setForceDischargeSoc(int forceDischargeSoc) {
			this.forceDischargeSoc = forceDischargeSoc;
			return this;
		}

		public Builder setForceDischargePower(int forceDischargePower) {
			this.forceDischargePower = forceDischargePower;
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
	public HybridEssMode hybridEssMode() {
		return this.builder.hybridEssMode;
	}

	@Override
	public String ess_id() {
		return this.builder.essId;
	}

	@Override
	public int maxSoc() {
		return this.builder.maxSoc;
	}

	@Override
	public int forceDischargeSoc() {
		return this.builder.forceDischargeSoc;
	}

	@Override
	public int forceDischargePower() {
		return this.builder.forceDischargePower;
	}

}
