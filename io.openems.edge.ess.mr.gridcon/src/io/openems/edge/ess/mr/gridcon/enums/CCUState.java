package io.openems.edge.ess.mr.gridcon.enums;

/**
 * This enum describes the overall total state of a gridcon.
 */
public enum CCUState {

	IDLE(),
	PRECHARGE(),
	STOP_PRECHARGE(),
	READY(),
	PAUSE(),
	RUN(),
	ERROR(),
	VOLTAGE_RAMPING_UP(),
	OVERLOAD(),
	SHORT_CIRCUIT_DETECTED(),
	DERATING_POWER(),
	DERATING_HARMONICS(),
	SIA_ACTIVE(),
	UNDEFINED();

	@Override
	public String toString() {
		return this.name();
	}
}
