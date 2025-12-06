package io.openems.edge.evse.chargepoint.abl.enums;

import io.openems.common.types.OptionsEnum;

/**
 * ABL EVCC2/3 Charging States as defined in section 3.4 of the specification.
 */
public enum ChargingState implements OptionsEnum {
	UNDEFINED(-1, "Undefined", Status.UNDEFINED), //

	/** Waiting for EV. */
	A1(0xA1, "A1 - Waiting for EV", Status.NOT_READY_FOR_CHARGING), //

	/** EV is asking for charging. */
	B1(0xB1, "B1 - EV asking for charging", Status.READY_FOR_CHARGING), //

	/** EV has the permission to charge. */
	B2(0xB2, "B2 - EV has permission to charge", Status.READY_FOR_CHARGING), //

	/** EV is charged. */
	C2(0xC2, "C2 - EV is charging", Status.CHARGING), //

	/** C2, reduced current (error F16, F17). */
	C3(0xC3, "C3 - Charging reduced (F16/F17)", Status.CHARGING), //

	/** C2, reduced current (imbalance F15). */
	C4(0xC4, "C4 - Charging reduced (imbalance F15)", Status.CHARGING), //

	/** Outlet disabled. */
	E0(0xE0, "E0 - Outlet disabled", Status.NOT_READY_FOR_CHARGING), //

	/** Production test. */
	E1(0xE1, "E1 - Production test", Status.NOT_READY_FOR_CHARGING), //

	/** EVCC setup mode. */
	E2(0xE2, "E2 - EVCC setup mode", Status.NOT_READY_FOR_CHARGING), //

	/** Bus idle. */
	E3(0xE3, "E3 - Bus idle", Status.NOT_READY_FOR_CHARGING), //

	/** Unintended closed contact (Welding). */
	F1(0xF1, "F1 - Unintended closed contact (Welding)", Status.ERROR), //

	/** Internal error. */
	F2(0xF2, "F2 - Internal error", Status.ERROR), //

	/** DC residual current detected. */
	F3(0xF3, "F3 - DC residual current detected", Status.ERROR), //

	/** Upstream communication timeout. */
	F4(0xF4, "F4 - Upstream communication timeout", Status.ERROR), //

	/** Lock of socket failed. */
	F5(0xF5, "F5 - Lock of socket failed", Status.ERROR), //

	/** CS out of range. */
	F6(0xF6, "F6 - CS out of range", Status.ERROR), //

	/** State D requested by EV. */
	F7(0xF7, "F7 - State D requested by EV", Status.ERROR), //

	/** CP out of range. */
	F8(0xF8, "F8 - CP out of range", Status.ERROR), //

	/** Overcurrent detected. */
	F9(0xF9, "F9 - Overcurrent detected", Status.ERROR), //

	/** Temperature outside limits. */
	F10(0xFA, "F10 - Temperature outside limits", Status.ERROR), //

	/** Unintended opened contact. */
	F11(0xFB, "F11 - Unintended opened contact", Status.ERROR); //

	private final int value;
	private final String name;
	public final Status status;

	private ChargingState(int value, String name, Status status) {
		this.value = value;
		this.name = name;
		this.status = status;
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
	 * Get ChargingState from integer value.
	 *
	 * @param value the state value
	 * @return the corresponding ChargingState or UNDEFINED
	 */
	public static ChargingState fromValue(int value) {
		for (ChargingState state : values()) {
			if (state.value == value) {
				return state;
			}
		}
		return UNDEFINED;
	}
}
