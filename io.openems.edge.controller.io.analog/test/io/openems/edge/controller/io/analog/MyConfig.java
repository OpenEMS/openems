package io.openems.edge.controller.io.analog;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.utils.ConfigUtils;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String analogOutputId;
		private Mode mode;
		private int manualTarget;
		private int maximumPower;
		private PowerBehavior powerBehaviour;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public MyConfig build() {
			return new MyConfig(this);
		}

		public Builder setAnalogOutputId(String analogOutputId) {
			this.analogOutputId = analogOutputId;
			return this;
		}

		public Builder setMode(Mode mode) {
			this.mode = mode;
			return this;
		}

		public Builder setManualTarget(int manualTarget) {
			this.manualTarget = manualTarget;
			return this;
		}

		public Builder setMaximumPower(int maximumPower) {
			this.maximumPower = maximumPower;
			return this;
		}

		public Builder setPowerBehaviour(PowerBehavior powerBehaviour) {
			this.powerBehaviour = powerBehaviour;
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
	public String analogOutput_id() {
		return this.builder.analogOutputId;
	}

	@Override
	public String analogOutput_target() {
		return ConfigUtils.generateReferenceTargetFilter(this.id(), this.analogOutput_id());
	}

	@Override
	public Mode mode() {
		return this.builder.mode;
	}

	@Override
	public int manualTarget() {
		return this.builder.manualTarget;
	}

	@Override
	public int maximumPower() {
		return this.builder.maximumPower;
	}

	@Override
	public PowerBehavior powerBehaviour() {
		return this.builder.powerBehaviour;
	}
}