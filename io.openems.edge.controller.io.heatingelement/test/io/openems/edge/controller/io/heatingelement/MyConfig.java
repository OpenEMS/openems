package io.openems.edge.controller.io.heatingelement;

import io.openems.edge.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
class MyConfig extends AbstractComponentConfig implements Config {

	private final String inputChannelAddress;
	private final String outputChannelPhaseL1;
	private final String outputChannelPhaseL2;
	private final String outputChannelPhaseL3;
	private final int powerOfPhase;
	private final Mode mode;
	private final WorkMode workMode;
	private final int minTime;
	private final String endTime;
	private final Level defaultLevel;
	private final int minimumSwitchingTime;

	public MyConfig(String id, String inputChannelAddress, String outputChannelPhaseL1, String outputChannelPhaseL2,
			String outputChannelPhaseL3, String endtime, int powerOfPhase, Mode mode, Level defaultLevel,
			WorkMode workMode, int minTime, int minimumSwitchingTime) {
		super(Config.class, id);
		this.inputChannelAddress = inputChannelAddress;
		this.outputChannelPhaseL1 = outputChannelPhaseL1;
		this.outputChannelPhaseL2 = outputChannelPhaseL2;
		this.outputChannelPhaseL3 = outputChannelPhaseL3;
		this.powerOfPhase = powerOfPhase;
		this.mode = mode;
		this.workMode = workMode;
		this.minTime = minTime;
		this.endTime = endtime;
		this.defaultLevel = defaultLevel;
		this.minimumSwitchingTime = minimumSwitchingTime;
	}

	@Override
	public int powerPerPhase() {
		return this.powerOfPhase;
	}

	@Override
	public String outputChannelPhaseL1() {
		return this.outputChannelPhaseL1;
	}

	@Override
	public String outputChannelPhaseL2() {
		return this.outputChannelPhaseL2;
	}

	@Override
	public String outputChannelPhaseL3() {
		return this.outputChannelPhaseL3;
	}

	@Override
	public Mode mode() {
		return this.mode;
	}

	@Override
	public WorkMode workMode() {
		return this.workMode;
	}

	@Override
	public int minTime() {
		return this.minTime;
	}

	@Override
	public String endTime() {
		return this.endTime;
	}

	@Override
	public Level defaultLevel() {
		return this.defaultLevel;
	}

	@Override
	public int minimumSwitchingTime() {
		return this.minimumSwitchingTime;
	}
}