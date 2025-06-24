package io.openems.edge.evse.chargepoint.heidelberg.connect.enums;

import java.util.Arrays;
import java.util.Objects;

import io.openems.common.types.OptionsEnum;

public enum ChargingState implements OptionsEnum {

	UNDEFINED(1, "Undefined / pre‐A1", Status.NOT_READY_FOR_CHARGING), //

	/**
	 * State A1.
	 * 
	 * <p>
	 * Car: No vehicle plugged, Wallbox: Wallbox doesn't allow charging
	 */
	A1(2, "A1", Status.NOT_READY_FOR_CHARGING), //

	/**
	 * State A2.
	 * 
	 * <p>
	 * Car: No vehicle plugged, Wallbox: Wallbox allows charging
	 */
	A2(3, "A2", Status.NOT_READY_FOR_CHARGING), //

	/**
	 * State B1.
	 * 
	 * <p>
	 * Car: Vehicle plugged without charging request, Wallbox: Wallbox doesn't allow
	 * charging
	 */
	B1(4, "B1", Status.CHARGING_REJECTED), //

	/**
	 * State B2.
	 * 
	 * <p>
	 * Car: Vehicle plugged without charging request, Wallbox: Wallbox allows
	 * charging
	 */
	B2(5, "B2", Status.NOT_READY_FOR_CHARGING), //

	/**
	 * State C1.
	 * 
	 * <p>
	 * Car: Vehicle plugged with charging request, Wallbox: Wallbox doesn't allow
	 * charging
	 */
	C1(6, "C1", Status.CHARGING_REJECTED), //

	/**
	 * State C2.
	 * 
	 * <p>
	 * Car: Vehicle plugged with charging request, Wallbox: Wallbox allows charging
	 */
	C2(7, "C2", Status.CHARGING), //

	/**
	 * [State D].
	 * 
	 * <p>
	 * Car: Vehicle plugged with charging request, Wallbox: Derating
	 */
	D(8, "Derating", Status.CHARGING), //

	/**
	 * State E.
	 * 
	 * <p>
	 * Car: Error Wallbox: Error
	 */
	E(9, "E", Status.ERROR), //

	/**
	 * State F.
	 * 
	 * <p>
	 * Car: Error, Wallbox: Wallbox locked or not ready
	 */
	F(10, "F", Status.NOT_READY_FOR_CHARGING), //

	/**
	 * [State F2].
	 * 
	 * <p>
	 * Car: Error, Wallbox: Error
	 */
	F2(11, "Car Error", Status.ERROR) //
	;

	private final int value;
	private final String name;
	public final Status state;

	private ChargingState(int value, String name, Status state) {
		this.value = value;
		this.name = name;
		this.state = state;
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
	 * State from value.
	 * 
	 * @param val value
	 * @return Charging State
	 */
	public static ChargingState stateFromValue(Integer val) {
		return Arrays.stream(ChargingState.values()) //
				.filter(s -> Objects.equals(s.value, val)) //
				.findFirst() //
				.orElse(UNDEFINED);
	}
}
