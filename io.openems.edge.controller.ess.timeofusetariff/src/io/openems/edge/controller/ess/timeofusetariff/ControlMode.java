package io.openems.edge.controller.ess.timeofusetariff;

public enum ControlMode {
	/**
	 * Delays discharge during low-price hours.
	 */
	DELAY_DISCHARGE(//
			StateMachine.BALANCING, //
			StateMachine.DELAY_DISCHARGE //
	),
	/**
	 * Active Charge from grid.
	 */
	CHARGE_CONSUMPTION(//
			StateMachine.BALANCING, //
			StateMachine.DELAY_DISCHARGE, //
			StateMachine.CHARGE //
	);

	public final StateMachine[] states;

	private ControlMode(StateMachine... states) {
		this.states = states;
	}
}