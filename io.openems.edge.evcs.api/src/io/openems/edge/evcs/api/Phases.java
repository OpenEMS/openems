package io.openems.edge.evcs.api;

import java.util.function.Function;

import io.openems.common.types.OptionsEnum;

public enum Phases implements OptionsEnum {
	ONE_PHASE(1, "One Phase", (max) -> Math.round(max / 3F), (minCurr) -> minCurr / 1000 * Evcs.DEFAULT_VOLTAGE,
			(maxCurr) -> maxCurr / 1000 * Evcs.DEFAULT_VOLTAGE), //
	TWO_PHASE(2, "Two Phase", (max) -> Math.round((max / 3F) * 2),
			(minCurr) -> minCurr / 1000 * Evcs.DEFAULT_VOLTAGE * 2,
			(maxCurr) -> maxCurr / 1000 * Evcs.DEFAULT_VOLTAGE * 2), //
	THREE_PHASE(3, "Three Phase", (max) -> max, (minCurr) -> minCurr / 1000 * Evcs.DEFAULT_VOLTAGE * 3,
			(maxCurr) -> maxCurr / 1000 * Evcs.DEFAULT_VOLTAGE * 3); //

	private final int value;
	private final String name;

	/**
	 * Calculating the power of a maximum given for three phases.
	 */
	private final Function<Integer, Integer> convertThreePhaseValue;

	/**
	 * Calculating the minimum power requirement.
	 */
	private final Function<Integer, Integer> calculateMinPower;

	/**
	 * Calculating the minimum power requirement.
	 */
	private final Function<Integer, Integer> calculateMaxPower;

	private Phases(int value, String name, Function<Integer, Integer> convertThreePhaseValue,
			Function<Integer, Integer> calculateMinPower, Function<Integer, Integer> calculateMaxPower) {
		this.value = value;
		this.name = name;
		this.convertThreePhaseValue = convertThreePhaseValue;
		this.calculateMinPower = calculateMinPower;
		this.calculateMaxPower = calculateMaxPower;
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

	/**
	 * Get the minimum power.
	 * 
	 * @param minimumCurrent minimum current (most likely 6 A)
	 * @return minimum power
	 */
	public int getMinimumPower(int minimumCurrent) {
		return this.calculateMinPower.apply(minimumCurrent).intValue();
	}

	/**
	 * Get the maximum power.
	 * 
	 * @param maximumCurrent maximum current (most likely 16 or 32 A)
	 * @return maximum power
	 */
	public int getMaximumPower(int maximumCurrent) {
		return this.calculateMaxPower.apply(maximumCurrent).intValue();
	}

	@Override
	public OptionsEnum getUndefined() {
		return THREE_PHASE;
	}

	/**
	 * Get preferred phase behavior.
	 * 
	 * @param power         target power in W
	 * @param currentPhases current phases
	 * @param minCurrent    minimum current in mA
	 * @param maxCurrent    minimum current in mA
	 * @return preferred phase behavior
	 */
	public static Phases preferredPhaseBehavior(int power, Phases currentPhases, int minCurrent, int maxCurrent) {

		// Keep current behavior if range is OK
		if (isBetween(currentPhases.getMinimumPower(minCurrent), currentPhases.getMaximumPower(maxCurrent), power)) {
			return currentPhases;
		}

		if (power > THREE_PHASE.getMaximumPower(maxCurrent)) {
			return THREE_PHASE;
		}

		if (power < ONE_PHASE.getMinimumPower(maxCurrent)) {
			return ONE_PHASE /* But no charge power possible for now */;
		}

		return switch (currentPhases) {
		case ONE_PHASE -> THREE_PHASE;
		case THREE_PHASE -> ONE_PHASE;
		case TWO_PHASE -> {
			if (isBetween(THREE_PHASE.getMinimumPower(minCurrent), THREE_PHASE.getMaximumPower(maxCurrent), power)) {
				yield THREE_PHASE;
			}
			yield ONE_PHASE /* Either none or one phase charge */;
		}
		};
	}

	private static boolean isBetween(int lowLimit, int highLimit, int value) {
		return value >= lowLimit && value <= highLimit;
	}
}