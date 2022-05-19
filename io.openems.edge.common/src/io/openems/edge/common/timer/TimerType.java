package io.openems.edge.common.timer;

/**
 * The TimerType. Determines what Type of Timer we have. Usually only needed by
 * CommunicationMaster and the Manager.
 */
public enum TimerType {
	COUNTING, TIME, CYCLES
}
