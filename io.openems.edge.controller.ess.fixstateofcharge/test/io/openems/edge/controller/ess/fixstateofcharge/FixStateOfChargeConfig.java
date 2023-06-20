package io.openems.edge.controller.ess.fixstateofcharge;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.utils.ConfigUtils;
import io.openems.edge.controller.ess.fixstateofcharge.api.EndCondition;

@SuppressWarnings("all")
public class FixStateOfChargeConfig extends AbstractComponentConfig implements ConfigFixStateOfCharge {

	protected static class Builder {
		private String id;
		private String essId;
		private boolean isRunning;
		private int targetSoc;
		private boolean targetTimeSpecified;
		private String targetTime;
		private int targetTimeBuffer;
		private boolean selfTermination;
		private int terminationBuffer;
		private boolean conditionalTermination;
		private EndCondition endCondition;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public FixStateOfChargeConfig build() {
			return new FixStateOfChargeConfig(this);
		}

		public Builder setEssId(String essId) {
			this.essId = essId;
			return this;
		}

		public Builder setRunning(boolean isRunning) {
			this.isRunning = isRunning;
			return this;
		}

		public Builder setTargetSoc(int targetSoc) {
			this.targetSoc = targetSoc;
			return this;
		}

		public Builder setSpecifyTargetTime(boolean targetTimeSpecified) {
			this.targetTimeSpecified = targetTimeSpecified;
			return this;
		}

		public Builder setTargetTime(String targetTime) {
			this.targetTime = targetTime;
			return this;
		}

		public Builder setTargetTimeBuffer(int targetTimeBuffer) {
			this.targetTimeBuffer = targetTimeBuffer;
			return this;
		}

		public Builder setSelfTermination(boolean selfTermination) {
			this.selfTermination = selfTermination;
			return this;
		}

		public Builder setTerminationBuffer(int terminationBuffer) {
			this.terminationBuffer = terminationBuffer;
			return this;
		}

		public Builder setConditionalTermination(boolean conditionalTermination) {
			this.conditionalTermination = conditionalTermination;
			return this;
		}

		public Builder setEndCondition(EndCondition endCondition) {
			this.endCondition = endCondition;
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

	private FixStateOfChargeConfig(Builder builder) {
		super(ConfigFixStateOfCharge.class, builder.id);
		this.builder = builder;
	}

	@Override
	public boolean isRunning() {
		return this.builder.isRunning;
	}

	@Override
	public int targetSoc() {
		return this.builder.targetSoc;
	}

	@Override
	public boolean targetTimeSpecified() {
		return this.builder.targetTimeSpecified;
	}

	@Override
	public String targetTime() {
		return this.builder.targetTime;
	}

	@Override
	public int targetTimeBuffer() {
		return this.builder.targetTimeBuffer;
	}

	@Override
	public boolean selfTermination() {
		return this.builder.selfTermination;
	}

	@Override
	public int terminationBuffer() {
		return this.builder.terminationBuffer;
	}

	@Override
	public boolean conditionalTermination() {
		return this.builder.conditionalTermination;
	}

	@Override
	public EndCondition endCondition() {
		return this.builder.endCondition;
	}

	@Override
	public String ess_id() {
		return this.builder.essId;
	}

	@Override
	public String ess_target() {
		return ConfigUtils.generateReferenceTargetFilter(this.id(), this.ess_id());
	}
}