package io.openems.edge.evcs.api;

import java.util.function.Function;

import io.openems.common.types.OptionsEnum;

public enum Phases implements OptionsEnum {
	ONE_PHASE(1, "One Phase", (max) -> Math.round(max / 3F)), //
	TWO_PHASE(2, "Two Phase", (max) -> Math.round((max / 3F) * 2)), //
	THREE_PHASE(3, "Three Phase", (max) -> max);

	private final int value;
	private final String name;

	/**
	 * Calculating the power of a maximum given for three phases.
	 */
	private final Function<Integer, Integer> convertThreePhaseValue;

	private Phases(int value, String name, Function<Integer, Integer> convertThreePhaseValue) {
		this.value = value;
		this.name = name;
		this.convertThreePhaseValue = convertThreePhaseValue;
	}

	@Override
	public int getValue() {
		return this.value;
	}

	@Override
	public String getName() {
		return this.name;
	}

	/**
	 * Get value converted from a three phase value.
	 * 
	 * <p>
	 * As the values e.g. hardware limit are most likely given for three phase
	 * charging, this is used to convert the value, depending on the current phases
	 * 
	 * @param threePhaseValue value using all three phases as power
	 * @return converted value
	 */
	public int getFromThreePhase(int threePhaseValue) {
		return this.convertThreePhaseValue.apply(threePhaseValue).intValue();
	}

	@Override
	public OptionsEnum getUndefined() {
		return THREE_PHASE;
	}
}