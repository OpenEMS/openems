package io.openems.edge.controller.symmetric.peakshaving;

import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String essId;
		private String meterId;
		private int peakShavingPower;
		private boolean enableRecharge;
		private int rechargePower;
		private boolean isStandalone;

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
		
		public Builder setEnableRecharge(boolean enableRecharge) {
			this.enableRecharge = enableRecharge;
			return this;
		}

		public Builder setRechargePower(int rechargePower) {
			this.rechargePower = rechargePower;
			return this;
		}
		
		public Builder setIsStandalone(boolean isStandalone) {
			this.isStandalone = isStandalone;
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
	public boolean enableRecharge() {
		return this.builder.enableRecharge;
	}

	@Override
	public int rechargePower() {
		return this.builder.rechargePower;
	}
	
	@Override
	public boolean isStandalone() {
		return this.builder.isStandalone;
	}

}