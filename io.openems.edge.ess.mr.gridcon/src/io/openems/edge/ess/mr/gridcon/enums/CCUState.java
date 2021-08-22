package io.openems.edge.ess.mr.gridcon.enums;

import io.openems.common.types.OptionsEnum;

/**
 * This enum describes the overall total state of a gridcon.
 */
public enum CcuState implements OptionsEnum {
	INIT(1, "INIT"), // = 1, // Initialisierung / Booting in Progress
	IDLE_CURRENTLY_NOT_WORKING(2, "IDLE"), // = 2,
	// System waiting for Instructions, MainContactor open, IGBT are not working
	PRECHARGE_CURRENTLY_NOT_WORKING(3, "PRECHARGE"), // = 3, // IPU StateObject
	GO_IDLE_CURRENTLY_NOT_WORKING(4, "GO_IDLE"), // = 4, // System changes STATE_IDLE
	CHARGED_CURRENTLY_NOT_WORKING(5, "CHARGED"), // = 5, // IPU StateObject
	READY_CURRENTLY_NOT_WORKING(6, "READY"), // = 6, // IPU StateObject
	RUN(7, "RUN"), // = 7, // IPU StateObject
	ERROR(8, "ERROR"), // = 8, // System in ErrorState
	PAUSE_CURRENTLY_NOT_WORKING(9, "PAUSE"), // = 9, // System Pause, Maincontactor closed, IGBTs are switching"
	SYNC_TO_V(10, "SYNC_TO_V"), // = 10, System synchronized to net
	BLACKSTART_CURRENTLY_NOT_WORKING(11, "BLACKSTART"), // = 11, // System is blackstarting
	COMPENSATOR(12, "COMPENSATOR"), // = 12, // System in netparallel mode
	ISLANDING(13, "ISLANDING"), // = 13, System in island mode
	UNDEFINED(-1, "UNDEFINED"),;

	private final int value;
	private final String name;

	private CcuState(int value, String name) {
		this.value = value;
		this.name = name;
	}

	@Override
	public int getValue() {
		return this.value;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}
}
