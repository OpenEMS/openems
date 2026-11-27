package io.openems.edge.evse.chargepoint.alfen;

import io.openems.common.types.OptionsEnum;

/**
 * State as reported by the Alfen charge point in Modbus register 1201 ("Mode 3
 * state").
 *
 * <p>
 * The digit distinguishes whether the charge point applies a PWM signal: "1"
 * means no PWM, i.e. charging is currently not allowed; "2" means PWM is
 * applied. Writing a set-point of zero switches the PWM off, so "B1" and "C1"
 * are the regular states while OpenEMS pauses a charging session - they must
 * still be considered ready for charging i.g. (to be tested).
 */
public enum Mode3State implements OptionsEnum {
	UNDEFINED(-1, "Undefined", false, false), //
	A(0, "A: No EV connected", false, false), //
	B1(1, "B1: EV connected, charging not allowed", true, true), //
	B2(2, "B2: EV connected, charging allowed", true, true), //
	C1(3, "C1: EV ready to charge, charging not allowed", true, true), //
	C2(4, "C2: Charging", true, true), //
	D1(5, "D1: EV ready to charge with ventilation, charging not allowed", true, true), //
	D2(6, "D2: Charging with ventilation", true, true), //
	E(7, "E: Error - short circuit or no power", false, false), //
	F(8, "F: Error - charge point unavailable", false, false) //
	;

	public final boolean isEvConnected;
	public final boolean isReadyForCharging;

	private final int value;
	private final String name;

	private Mode3State(int value, String name, boolean isEvConnected, boolean isReadyForCharging) {
		this.value = value;
		this.name = name;
		this.isEvConnected = isEvConnected;
		this.isReadyForCharging = isReadyForCharging;
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
	 * Parses the raw String of Modbus register 1201.
	 *
	 * @param value the raw register value
	 * @return the {@link Mode3State}; {@link #UNDEFINED} for unknown values
	 */
	public static Mode3State fromString(String value) {
		if (value == null) {
			return UNDEFINED;
		}
		return switch (value.trim().toUpperCase()) {
		case "A" -> A;
		case "B1" -> B1;
		case "B2" -> B2;
		case "C1" -> C1;
		case "C2" -> C2;
		case "D1" -> D1;
		case "D2" -> D2;
		case "E" -> E;
		case "F" -> F;
		default -> UNDEFINED;
		};
	}
}
