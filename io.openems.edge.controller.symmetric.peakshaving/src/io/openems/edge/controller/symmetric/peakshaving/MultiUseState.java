package io.openems.edge.controller.symmetric.peakshaving;

import io.openems.common.types.OptionsEnum;

enum MultiUseState implements OptionsEnum {
	/**
	 * If no multi use is active, this controller will set a fixed value for active
	 * power, which no follow up controller can override.
	 */
	NONE(0, "No multi use is currently allowed"),
	/**
	 * If parallel multi use is active, this controller sets a minimum value, which
	 * allows follow up controllers to override another value, which applies to the
	 * given constraints.
	 */
	PARALLEL(1, "SoC based parallel multi use is allowed");

	private final int value;
	private final String name;

	private MultiUseState(int value, String name) {
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
		return NONE;
	}

	/**
	 * Returns the desired multi use behavior based on the input values.
	 * 
	 * @param soc       The current state of charge.
	 * @param minSoc    The required state of charge for this peak shaving multi use
	 *                  case.
	 * @param socBuffer The hysteresis buffer to add on top of the SoC before
	 *                  allowing parallel multi use.
	 * @return The new multi use state.
	 */
	public MultiUseState getMultiUseBehavior(int soc, int minSoc, int socBuffer) {

		return switch (this) {
		case NONE -> {
			if (soc >= minSoc + socBuffer) {
				yield MultiUseState.PARALLEL;
			}
			yield MultiUseState.NONE;
		}
		case PARALLEL -> {
			if (soc <= minSoc) {
				yield MultiUseState.NONE;
			}
			yield MultiUseState.PARALLEL;
		}
		};
	}
}