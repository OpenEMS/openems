package io.openems.edge.controller.ess.timeofusetariff;

import static com.google.common.collect.ImmutableList.toImmutableList;

import java.util.Arrays;

import com.google.common.collect.ImmutableList;

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

	public final ImmutableList<StateMachine> modes;
	@Deprecated
	public final StateMachine[] modesArray;

	private ControlMode(StateMachine... modes) {
		this.modesArray = modes;
		this.modes = Arrays.stream(modes) //
				.collect(toImmutableList());
	}
}