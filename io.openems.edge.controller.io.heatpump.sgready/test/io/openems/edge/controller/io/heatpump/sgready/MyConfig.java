package io.openems.edge.controller.io.heatpump.sgready;

import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String outputChannel1;
		private String outputChannel2;
		private Mode mode;
		private Status manualState = Status.REGULAR;
		private boolean automaticRecommendationCtrlEnabled;
		private int automaticRecommendationSurplusPower;
		private boolean automaticForceOnCtrlEnabled;
		private int automaticForceOnSurplusPower;
		private int automaticForceOnSoc;
		private boolean automaticLockCtrlEnabled;
		private int automaticLockGridBuyPower;
		private int automaticLockSoc;
		private int minimumSwitchingTime;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public MyConfig build() {
			return new MyConfig(this);
		}

		public Builder setOutputChannel1(String outputChannel1) {
			this.outputChannel1 = outputChannel1;
			return this;
		}

		public Builder setOutputChannel2(String outputChannel2) {
			this.outputChannel2 = outputChannel2;
			return this;
		}

		public Builder setMode(Mode mode) {
			this.mode = mode;
			return this;
		}

		public Builder setManualState(Status manualState) {
			this.manualState = manualState;
			return this;
		}

		public Builder setAutomaticRecommendationCtrlEnabled(boolean automaticRecommendationCtrlEnabled) {
			this.automaticRecommendationCtrlEnabled = automaticRecommendationCtrlEnabled;
			return this;
		}

		public Builder setAutomaticRecommendationSurplusPower(int automaticRecommendationSurplusPower) {
			this.automaticRecommendationSurplusPower = automaticRecommendationSurplusPower;
			return this;
		}

		public Builder setAutomaticForceOnCtrlEnabled(boolean automaticForceOnCtrlEnabled) {
			this.automaticForceOnCtrlEnabled = automaticForceOnCtrlEnabled;
			return this;
		}

		public Builder setAutomaticForceOnSurplusPower(int automaticForceOnSurplusPower) {
			this.automaticForceOnSurplusPower = automaticForceOnSurplusPower;
			return this;
		}

		public Builder setAutomaticForceOnSoc(int automaticForceOnSoc) {
			this.automaticForceOnSoc = automaticForceOnSoc;
			return this;
		}

		public Builder setAutomaticLockCtrlEnabled(boolean automaticLockCtrlEnabled) {
			this.automaticLockCtrlEnabled = automaticLockCtrlEnabled;
			return this;
		}

		public Builder setAutomaticLockGridBuyPower(int automaticLockGridBuyPower) {
			this.automaticLockGridBuyPower = automaticLockGridBuyPower;
			return this;
		}

		public Builder setAutomaticLockSoc(int automaticLockSoc) {
			this.automaticLockSoc = automaticLockSoc;
			return this;
		}

		public Builder setMinimumSwitchingTime(int minimumSwitchingTime) {
			this.minimumSwitchingTime = minimumSwitchingTime;
			return this;
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
	public String outputChannel1() {
		return this.builder.outputChannel1;
	}

	@Override
	public String outputChannel2() {
		return this.builder.outputChannel2;
	}

	@Override
	public Mode mode() {
		return this.builder.mode;
	}

	@Override
	public Status manualState() {
		return this.builder.manualState;
	}

	@Override
	public boolean automaticRecommendationCtrlEnabled() {
		return this.builder.automaticRecommendationCtrlEnabled;
	}

	@Override
	public int automaticRecommendationSurplusPower() {
		return this.builder.automaticRecommendationSurplusPower;
	}

	@Override
	public boolean automaticForceOnCtrlEnabled() {
		return this.builder.automaticForceOnCtrlEnabled;
	}

	@Override
	public int automaticForceOnSurplusPower() {
		return this.builder.automaticForceOnSurplusPower;
	}

	@Override
	public int automaticForceOnSoc() {
		return this.builder.automaticForceOnSoc;
	}

	@Override
	public boolean automaticLockCtrlEnabled() {
		return this.builder.automaticLockCtrlEnabled;
	}

	@Override
	public int automaticLockGridBuyPower() {
		return this.builder.automaticLockGridBuyPower;
	}

	@Override
	public int automaticLockSoc() {
		return this.builder.automaticLockSoc;
	}

	@Override
	public int minimumSwitchingTime() {
		return this.builder.minimumSwitchingTime;
	}

	@Override
	public boolean debugMode() {
		return false;
	}
}