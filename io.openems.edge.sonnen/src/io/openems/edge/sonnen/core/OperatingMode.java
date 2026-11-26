package io.openems.edge.sonnen.core;

import io.openems.common.types.OptionsEnum;

/**
 * Operating modes of the sonnenBatterie, as defined by the 'EM_OperatingMode'
 * configuration of the local JSON-API.
 */
public enum OperatingMode implements OptionsEnum {

	/** Manual charging or discharging via API. */
	MANUAL(1),
	/** Automatic Self-Consumption. Default. */
	SELF_CONSUMPTION(2),
	/** Battery-Module-Extension (30 %). */
	BATTERY_MODULE_EXTENSION(6),
	/** Time-of-Use. */
	TIME_OF_USE(10),
	/** Unknown mode. */
	UNDEFINED(-1);

	private final int value;

	private OperatingMode(int value) {
		this.value = value;
	}

	@Override
	public int getValue() {
		return this.value;
	}

	@Override
	public String getName() {
		return this.name();
	}

	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}

	/**
	 * Gets the {@link OperatingMode} for the given API value.
	 *
	 * @param value the API value
	 * @return the {@link OperatingMode}; {@link #UNDEFINED} if unknown
	 */
	public static OperatingMode fromValue(int value) {
		for (var mode : OperatingMode.values()) {
			if (mode.value == value) {
				return mode;
			}
		}
		return UNDEFINED;
	}
}
