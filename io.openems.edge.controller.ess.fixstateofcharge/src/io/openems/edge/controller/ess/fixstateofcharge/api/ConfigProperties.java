package io.openems.edge.controller.ess.fixstateofcharge.api;

public class ConfigProperties {

	// Is Running. Set controller ON or OFF
	private boolean isRunning;

	// Target SoC
	private int targetSoc;

	// Target time specified
	private boolean targetTimeSpecified;

	// Target date [DD.MM.YYYY]
	private String targetDate;

	// Target time [HH:MM]
	private String targetTime;

	// Target time buffer
	int targetTimeBuffer;

	// Terminates itself at the end
	private boolean selfTermination;

	// Terminate time buffer in min
	private int terminationBuffer;

	// Terminates itself after separate conditon
	private boolean conditionalTermination;

	// Condition for termination
	private EndCondition endCondition;

	public ConfigProperties(boolean isRunning, int targetSoc, boolean targetTimeSpecified, String targetDate,
			String targetTime, int targetTimeBuffer, boolean selfTermination, int terminationBuffer,
			boolean conditionalTermination, EndCondition endCondition) {
		this.isRunning = isRunning;
		this.targetSoc = targetSoc;
		this.targetTimeSpecified = targetTimeSpecified;
		this.targetDate = targetDate;
		this.targetTime = targetTime;
		this.targetTimeBuffer = targetTimeBuffer;
		this.selfTermination = selfTermination;
		this.terminationBuffer = terminationBuffer;
		this.conditionalTermination = conditionalTermination;
		this.endCondition = endCondition;
	}

	public boolean isRunning() {
		return this.isRunning;
	}

	public int getTargetSoc() {
		return this.targetSoc;
	}

	public boolean isTargetTimeSpecified() {
		return this.targetTimeSpecified;
	}

	public String getTargetDate() {
		return this.targetDate;
	}

	public String getTargetTime() {
		return this.targetTime;
	}

	public int getTargetTimeBuffer() {
		return this.targetTimeBuffer;
	}

	public boolean isSelfTermination() {
		return this.selfTermination;
	}

	public int getTerminationBuffer() {
		return this.terminationBuffer;
	}

	public boolean isConditionalTermination() {
		return this.conditionalTermination;
	}

	public EndCondition getEndCondition() {
		return this.endCondition;
	}

}
