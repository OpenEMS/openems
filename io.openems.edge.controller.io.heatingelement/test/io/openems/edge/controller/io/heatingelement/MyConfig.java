package io.openems.edge.controller.io.heatingelement;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.edge.controller.io.heatingelement.enums.Level;
import io.openems.edge.controller.io.heatingelement.enums.Mode;
import io.openems.edge.controller.io.heatingelement.enums.WorkMode;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String outputChannelPhaseL1;
		private String outputChannelPhaseL2;
		private String outputChannelPhaseL3;
		private int powerOfPhase;
		private Mode mode;
		private WorkMode workMode;
		private int minTime;
		private String endTime;
		private Level defaultLevel;
		private int minimumSwitchingTime;

		private Builder() {

		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setOutputChannelPhaseL1(String outputChannelPhaseL1) {
			this.outputChannelPhaseL1 = outputChannelPhaseL1;
			return this;
		}

		public Builder setOutputChannelPhaseL2(String outputChannelPhaseL2) {
			this.outputChannelPhaseL2 = outputChannelPhaseL2;
			return this;
		}

		public Builder setOutputChannelPhaseL3(String outputChannelPhaseL3) {
			this.outputChannelPhaseL3 = outputChannelPhaseL3;
			return this;
		}

		public Builder setPowerOfPhase(int powerOfPhase) {
			this.powerOfPhase = powerOfPhase;
			return this;
		}

		public Builder setMode(Mode mode) {
			this.mode = mode;
			return this;
		}

		public Builder setWorkMode(WorkMode workMode) {
			this.workMode = workMode;
			return this;
		}

		public Builder setMinTime(int minTime) {
			this.minTime = minTime;
			return this;
		}

		public Builder setEndTime(String endTime) {
			this.endTime = endTime;
			return this;
		}

		public Builder setDefaultLevel(Level defaultLevel) {
			this.defaultLevel = defaultLevel;
			return this;
		}

		public Builder setMinimumSwitchingTime(int minimumSwitchingTime) {
			this.minimumSwitchingTime = minimumSwitchingTime;
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
	public Mode mode() {
		return this.builder.mode;
	}

	@Override
	public String outputChannelPhaseL1() {
		return this.builder.outputChannelPhaseL1;
	}

	@Override
	public String outputChannelPhaseL2() {
		return this.builder.outputChannelPhaseL2;
	}

	@Override
	public String outputChannelPhaseL3() {
		return this.builder.outputChannelPhaseL3;
	}

	@Override
	public Level defaultLevel() {
		return this.builder.defaultLevel;
	}

	@Override
	public String endTime() {
		return this.builder.endTime;
	}

	@Override
	public WorkMode workMode() {
		return this.builder.workMode;
	}

	@Override
	public int minTime() {
		return this.builder.minTime;
	}

	@Override
	public int powerPerPhase() {
		return this.builder.powerOfPhase;
	}

	@Override
	public int minimumSwitchingTime() {
		return this.builder.minimumSwitchingTime;
	}

}