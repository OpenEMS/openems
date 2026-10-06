package io.openems.edge.evse.chargepoint.voltie.enums;

import io.openems.common.types.OptionsEnum;

/**
 * EVSE_STATE (register 0x000A) as reported by the Voltie charge-point.
 *
 * <p>
 * Source: Voltie Modbus API documentation v1.4 (2026-09-23). Only the codes
 * listed in that document are enumerated here; any other non-zero value is
 * handled defensively by {@link #isErrorValue(Integer)}.
 */
public enum EvseState implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	/** State not yet determined by the charger. */
	UNKNOWN(0x00, "State not yet determined"), //
	/** Vehicle state A: no vehicle connected. */
	STATE_A(0x01, "Vehicle state A, not connected"), //
	/** Vehicle state B: vehicle connected, ready. */
	STATE_B(0x02, "Vehicle state B, connected"), //
	/** Vehicle state C: charging. */
	STATE_C(0x03, "Vehicle state C, charging"), //
	/** Vehicle state D: charging with ventilation request. */
	STATE_D(0x04, "Vehicle state D, charging with ventilation"), //
	ERROR_CONTROL_PILOT(0x05, "Error: control signal (CP)"), //
	ERROR_RESIDUAL_CURRENT(0x06, "Error: residual current detected"), //
	ERROR_NO_GROUNDING(0x07, "Error: no grounding"), //
	ERROR_STUCK_RELAY(0x08, "Error: stuck relay"), //
	ERROR_RCD_SENSOR_TEST(0x09, "Error: residual current sensor test failed"), //
	ERROR_OVER_TEMPERATURE(0x0A, "Error: over temperature"), //
	ERROR_OVER_CURRENT(0x0B, "Error: over current"), //
	ERROR_I2C_BUS(0x0C, "Error: I2C bus fault"), //
	ERROR_VEHICLE(0x0D, "Error: vehicle fault (state E)"), //
	ERROR_OVER_HUMIDITY(0x0E, "Error: over humidity"), //
	ERROR_PHASE_MISCONNECTED(0x0F, "Error: phase misconnected"), //
	ERROR_OVERVOLTAGE(0x10, "Error: overvoltage"), //
	ERROR_UNDERVOLTAGE(0x11, "Error: undervoltage on AC supply"), //
	CHARGER_DISABLED(0x12, "Charger disabled, not functioning"), //
	BOOTING(0x13, "Booting"), //
	ERROR_UNKNOWN_POWER_BOARD(0x15, "Error: unknown power board"), //
	UNDETERMINED(0x18, "State undetermined"), //
	VOLTIEMETER_FIRMWARE_UPLOAD(0x19, "Uploading VoltieMeter firmware");

	private final int value;
	private final String name;

	private EvseState(int value, String name) {
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
		return UNDEFINED;
	}

	/**
	 * Is a vehicle connected in this state?.
	 *
	 * @return true for vehicle states B, C and D
	 */
	public boolean isEvConnected() {
		return switch (this) {
		case STATE_B, STATE_C, STATE_D -> true;
		default -> false;
		};
	}

	/**
	 * Is the charge-point actively charging in this state?.
	 *
	 * @return true for vehicle states C and D
	 */
	public boolean isCharging() {
		return switch (this) {
		case STATE_C, STATE_D -> true;
		default -> false;
		};
	}

	/**
	 * Is this an internal error state of the charge-point?.
	 *
	 * @return true for error states
	 */
	public boolean isError() {
		return switch (this) {
		case ERROR_CONTROL_PILOT, ERROR_RESIDUAL_CURRENT, ERROR_NO_GROUNDING, ERROR_STUCK_RELAY, //
				ERROR_RCD_SENSOR_TEST, ERROR_OVER_TEMPERATURE, ERROR_OVER_CURRENT, ERROR_I2C_BUS, //
				ERROR_VEHICLE, ERROR_OVER_HUMIDITY, ERROR_PHASE_MISCONNECTED, ERROR_OVERVOLTAGE, //
				ERROR_UNDERVOLTAGE, ERROR_UNKNOWN_POWER_BOARD ->
			true;
		default -> false;
		};
	}

	/**
	 * Evaluates whether a raw EVSE_STATE register value represents an error state.
	 *
	 * <p>
	 * Values that are not documented in this enum are defensively treated as
	 * errors: newer firmware may introduce additional error states.
	 *
	 * @param raw the raw register value; null if unknown
	 * @return true for error states
	 */
	public static boolean isErrorValue(Integer raw) {
		if (raw == null) {
			return false;
		}
		var state = OptionsEnum.getOptionOrUndefined(EvseState.class, raw);
		if (state != UNDEFINED) {
			return state.isError();
		}
		return true; // unknown state -> defensively an error
	}
}
