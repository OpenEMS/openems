package io.openems.edge.controller.symmetric.peakshaving;

import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String essId;
		private String meterId;
		private int peakShavingPower;
		private int rechargePower;
		private int minSocLimit;
		private boolean enableMultipleEssConstraints;
		private int socHysteresis;

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

		public Builder setMeterId(String meterId) {
			this.meterId = meterId;
			return this;
		}

		public Builder setPeakShavingPower(int peakShavingPower) {
			this.peakShavingPower = peakShavingPower;
			return this;
		}

		public Builder setRechargePower(int rechargePower) {
			this.rechargePower = rechargePower;
			return this;
		}
		
		public Builder setMinSocLimit(int minSocLimit) {
			this.minSocLimit = minSocLimit;
			return this;
		}

		public Builder setEnableMultipleEssConstraints(boolean enableMultipleEssConstraints) {
			this.enableMultipleEssConstraints = enableMultipleEssConstraints;
			return this;
		}

		public Builder setSocHysteresis(int socHysteresis) {
			this.socHysteresis = socHysteresis;
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
	public int peakShavingPower() {
		return this.builder.peakShavingPower;
	}

	@Override
	public int rechargePower() {
		return this.builder.rechargePower;
	}

	@Override
	public int minSocLimit() {
		return this.builder.minSocLimit;
	}

	@Override
	public boolean enableMultipleEssConstraints() {
		return this.builder.enableMultipleEssConstraints;
	}

	@Override
	public int socHysteresis() {
		return this.builder.socHysteresis;
	}

}