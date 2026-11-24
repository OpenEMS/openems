package io.openems.edge.controller.io.heatingelement;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.edge.controller.io.heatingelement.enums.Distribution;
import io.openems.edge.controller.io.heatingelement.enums.Level;
import io.openems.edge.controller.io.heatingelement.enums.Mode;
import io.openems.edge.controller.io.heatingelement.enums.NumberOfHeatingElements;
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
		private int minEnergylimit;
		private String endTimeWithMeter;
		private String meterId;
		private int minTime;
		private String endTime;
		private Level defaultLevel;
		private int minimumSwitchingTime;
		private String scheduler;
		private NumberOfHeatingElements numberOfHeatingElements = NumberOfHeatingElements.ONE;
		private int powerPerPhaseHeatingElementB;
		private String outputChannelPhaseL1HeatingElementB = "io0/Relay4";
		private String outputChannelPhaseL2HeatingElementB = "io0/Relay5";
		private String outputChannelPhaseL3HeatingElementB = "io0/Relay6";
		private Distribution distribution = Distribution.SEQUENTIAL;

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

		public Builder setMinEnergylimit(int minEnergylimit) {
			this.minEnergylimit = minEnergylimit;
			return this;
		}

		public Builder setMeterid(String meterid) {
			this.meterId = meterid;
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

		public Builder setEndTimeWithMeter(String endTimeWithMeter) {
			this.endTimeWithMeter = endTimeWithMeter;
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

		public Builder setScheduler(String scheduler) {
			this.scheduler = scheduler;
			return this;
		}

		public Builder setNumberOfHeatingElements(NumberOfHeatingElements numberOfHeatingElements) {
			this.numberOfHeatingElements = numberOfHeatingElements;
			return this;
		}

		public Builder setPowerPerPhaseHeatingElementB(int powerPerPhaseHeatingElementB) {
			this.powerPerPhaseHeatingElementB = powerPerPhaseHeatingElementB;
			return this;
		}

		public Builder setOutputChannelPhaseL1HeatingElementB(String outputChannelPhaseL1HeatingElementB) {
			this.outputChannelPhaseL1HeatingElementB = outputChannelPhaseL1HeatingElementB;
			return this;
		}

		public Builder setOutputChannelPhaseL2HeatingElementB(String outputChannelPhaseL2HeatingElementB) {
			this.outputChannelPhaseL2HeatingElementB = outputChannelPhaseL2HeatingElementB;
			return this;
		}

		public Builder setOutputChannelPhaseL3HeatingElementB(String outputChannelPhaseL3HeatingElementB) {
			this.outputChannelPhaseL3HeatingElementB = outputChannelPhaseL3HeatingElementB;
			return this;
		}

		public Builder setDistribution(Distribution distribution) {
			this.distribution = distribution;
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
	public int minEnergylimit() {
		return this.builder.minEnergylimit;
	}

	@Override
	public String meter_id() {
		return this.builder.meterId;
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

	@Override
	public String endTimeWithMeter() {
		return this.builder.endTimeWithMeter;
	}

	@Override
	public String schedule() {
		return this.builder.scheduler;
	}

	@Override
	public NumberOfHeatingElements numberOfHeatingElements() {
		return this.builder.numberOfHeatingElements;
	}

	@Override
	public int powerPerPhaseHeatingElementB() {
		return this.builder.powerPerPhaseHeatingElementB;
	}

	@Override
	public String outputChannelPhaseL1HeatingElementB() {
		return this.builder.outputChannelPhaseL1HeatingElementB;
	}

	@Override
	public String outputChannelPhaseL2HeatingElementB() {
		return this.builder.outputChannelPhaseL2HeatingElementB;
	}

	@Override
	public String outputChannelPhaseL3HeatingElementB() {
		return this.builder.outputChannelPhaseL3HeatingElementB;
	}

	@Override
	public Distribution distribution() {
		return this.builder.distribution;
	}
}