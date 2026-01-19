package io.openems.edge.controller.cleverpv;

import java.util.Optional;

import io.openems.common.types.OptionsEnum;

public enum PowerStorageState implements OptionsEnum {
	IDLE(0, "Idle"), //
	DISCHARGING(1, "Discharging"), //
	DISABLED(2, "Disabled"), //
	CHARGING(3, "Charging");

	public final int value;
	public final String name;

	private PowerStorageState(int value, String name) {
		this.value = value;
		this.name = name;
	}

	/**
	 * Maps the given power value to a PowerStorageState.
	 *
	 * @param power the ESS power
	 * @return the corresponding PowerStorageState
	 */
	public static PowerStorageState fromPower(Integer power) {
		if (power == null) {
			return DISABLED;
		}
		if (power > 0) {
			return CHARGING;
		}
		if (power < 0) {
			return DISCHARGING;
		}
		return IDLE;
	}

	public int getValue() {
		return this.value;
	}

	public String getName() {
		return this.name;
	}

	/**
	 * Gets the PowerStorageState from an integer value.
	 *
	 * @param value the integer value
	 * @return the PowerStorageState
	 */
	public static Optional<PowerStorageState> fromValue(int value) {
		for (PowerStorageState powerStorageState : PowerStorageState.values()) {
			if (value == powerStorageState.getValue()) {
				return Optional.of(powerStorageState);
			}
		}
		return Optional.empty();
	}

	@Override
	public OptionsEnum getUndefined() {
		return IDLE;
	}
}
