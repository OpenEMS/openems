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
			StateMachine.CHARGE_GRID //
	),
	/**
	 * Active Discharge to grid.
	 */
	DISCHARGE_TO_GRID(//
			StateMachine.BALANCING, //
			StateMachine.DELAY_DISCHARGE, //
			StateMachine.CHARGE_GRID, //
			StateMachine.DISCHARGE_GRID //
	);

	public final StateMachine[] modes;

	private ControlMode(StateMachine... modes) {
		this.modes = modes;
	}
}