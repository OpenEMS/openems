package io.openems.edge.bridge.eebus.usecase.powerlimitation.api;

public enum LimitPowerState {
	/**
	 * Controllable System starts in "init" state after completion of its (re)start;
	 * CS limited by the Failsafe Consumption Active Power Limit according to
	 * [LPC-901/1] and [LPC-901/2]. The Active Power Consumption Limit SHALL be
	 * deactivated ([LPC-009/2]).
	 */
	INIT,
	/**
	 * Controllable System is not limited, but still controlled by Actor Energy Guard
	 * (unlike state "unlimited/autonomous").
	 * The Active Power Consumption Limit SHALL be deactivated ([LPC-009/2]).
	 */
	CONTROLLED,
	/**
	 * Controllable System is in a limited state (controlled by the Actor Energy Guard) where a
	 * limited amount of power is consumed. The Active Power Consumption Limit SHALL be activated
	 * ([LPC-009/1]).
	 */
	LIMITED,
	/**
	 * Controllable System is in "failsafe state" (not controlled by the Energy Guard) where it
	 * is limited by the failsafe limit. The Active Power Consumption Limit SHALL be deactivated ([LPC-
	 * 009/2]).
	 */
	FAILSAFE,
	/**
	 * Controllable System is not limited and consumes power as if there would be no
	 * external power limitation available.
	 * The Active Power Consumption Limit SHALL be deactivated ([LPC-009/2])
	 */
	AUTONOMOUS

	;
	
}
