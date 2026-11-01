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
		private int minEnergylimit;
		private String endTimeWithMeter;
		private String meterId;
		private int minTime;
		private String endTime;
		private Level defaultLevel;
		private int minimumSwitchingTime;
		private String scheduler;
		private boolean hasWaterThermometer = false;
		private String waterThermometerId = "";
		private Level sensorErrorLevel = Level.LEVEL_0;
		private int lowWaterTemperature = 45;
		private int highWaterTemperature = 60;
		private int maxWaterTemperature = 85;
		private boolean enableLegionellenProtection = false;
		private int legionellenWaterTemperature = 65;
		private int legionellenInterval = 7;
		private int minLegionellenTime = 1;
		private int legionellenMaxHeatingTime = 6;

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

		public Builder setHasWaterThermometer(boolean hasWaterThermometer) {
			this.hasWaterThermometer = hasWaterThermometer;
			return this;
		}

		public Builder setWaterThermometerId(String waterThermometerId) {
			this.waterThermometerId = waterThermometerId;
			return this;
		}

		public Builder setSensorErrorLevel(Level sensorErrorLevel) {
			this.sensorErrorLevel = sensorErrorLevel;
			return this;
		}

		public Builder setWaterTemperatures(int low, int high, int max) {
			this.lowWaterTemperature = low;
			this.highWaterTemperature = high;
			this.maxWaterTemperature = max;
			return this;
		}

		public Builder setLegionellenProtection(boolean enabled, int temperature, int interval, int minTime,
				int maxHeatingTime) {
			this.enableLegionellenProtection = enabled;
			this.legionellenWaterTemperature = temperature;
			this.legionellenInterval = interval;
			this.minLegionellenTime = minTime;
			this.legionellenMaxHeatingTime = maxHeatingTime;
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
	public boolean hasWaterThermometer() {
		return this.builder.hasWaterThermometer;
	}

	@Override
	public String waterThermometer_id() {
		return this.builder.waterThermometerId;
	}

	@Override
	public Level sensorErrorLevel() {
		return this.builder.sensorErrorLevel;
	}

	@Override
	public int lowWaterTemperature() {
		return this.builder.lowWaterTemperature;
	}

	@Override
	public int highWaterTemperature() {
		return this.builder.highWaterTemperature;
	}

	@Override
	public int maxWaterTemperature() {
		return this.builder.maxWaterTemperature;
	}

	@Override
	public boolean enableLegionellenProtection() {
		return this.builder.enableLegionellenProtection;
	}

	@Override
	public int legionellenWaterTemperature() {
		return this.builder.legionellenWaterTemperature;
	}

	@Override
	public int legionellenInterval() {
		return this.builder.legionellenInterval;
	}

	@Override
	public int minLegionellenTime() {
		return this.builder.minLegionellenTime;
	}

	@Override
	public int legionellenMaxHeatingTime() {
		return this.builder.legionellenMaxHeatingTime;
	}
}
