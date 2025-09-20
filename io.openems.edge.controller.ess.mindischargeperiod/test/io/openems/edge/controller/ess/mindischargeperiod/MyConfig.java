package io.openems.edge.controller.ess.mindischargeperiod;

import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id = null;
		private int dischargeTime;
		private int minDischargePower;
		private int activateDischargePower;
		private String essId;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setDischargeTime(int dischargeTime) {
			this.dischargeTime = dischargeTime;
			return this;
		}

		public Builder setMinDischargePower(int minDischargePower) {
			this.minDischargePower = minDischargePower;
			return this;
		}

		public Builder setActivateDischargePower(int activateDischargePower) {
			this.activateDischargePower = activateDischargePower;
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
	public int activateDischargePower() {
		return this.builder.activateDischargePower;
	}

	@Override
	public int minDischargePower() {
		return this.builder.minDischargePower;
	}

	@Override
	public int dischargeTime() {
		return this.builder.dischargeTime;
	}

}