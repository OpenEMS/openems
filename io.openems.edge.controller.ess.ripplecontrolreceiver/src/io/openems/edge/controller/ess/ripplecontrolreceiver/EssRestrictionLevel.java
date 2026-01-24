package io.openems.edge.controller.ess.ripplecontrolreceiver;

import io.openems.common.types.OptionsEnum;

public enum EssRestrictionLevel implements OptionsEnum {

	NO_RESTRICTION(0, "No restriction", 100), //
	ZERO_PERCENT(1, "0% allowed grid feed-in", 0), //
	THIRTY_PERCENT(2, "30% allowed grid feed-in", 30), //
	SIXTY_PERCENT(3, "60% allowed grid feed-in", 60); //

	private final int value;
	private final int limitationFactor;
	private final String name;

	EssRestrictionLevel(int value, String name, int limitationFactor) {
		this.value = value;
		this.limitationFactor = limitationFactor;
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
		return NO_RESTRICTION;
	}

	public double getLimitationFactor() {
		return this.limitationFactor / 100.0;
	}

	/**
	 * Returns the highest priority restriction level that is active.
	 * 
	 * <p>
	 * Priority: 0% > 30% > 60%
	 * </p>
	 * 
	 * @param zeroPercentActive   input for 0% restriction active (true means
	 *                            external signal is active)
	 * @param thirtyPercentActive input for 30% restriction active (true means
	 *                            external signal is active)
	 * @param sixtyPercentActive  input for 60% restriction active (true means
	 *                            external signal is active)
	 * @return the highest priority active restriction level
	 */
	public static EssRestrictionLevel getRestrictionLevelByPriority(boolean zeroPercentActive,
			boolean thirtyPercentActive, boolean sixtyPercentActive) {
		if (zeroPercentActive) {
			return ZERO_PERCENT;
		}
		if (thirtyPercentActive) {
			return THIRTY_PERCENT;
		}
		if (sixtyPercentActive) {
			return SIXTY_PERCENT;
		}
		return NO_RESTRICTION;
	}
}
