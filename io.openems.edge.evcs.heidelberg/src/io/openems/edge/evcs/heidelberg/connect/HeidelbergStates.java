package io.openems.edge.evcs.heidelberg.connect;

import io.openems.common.types.OptionsEnum;

public enum HeidelbergStates implements OptionsEnum {

	UNDEFINED(-1, "Undefined"), //

	/**
	 * State A1.
	 * 
	 * <p>
	 * Car: No vehicle plugged, Wallbox: Wallbox doesn't allow charging
	 */
	A1(2, "A1"), //

	/**
	 * State A2.
	 * 
	 * <p>
	 * Car: No vehicle plugged, Wallbox: Wallbox allows charging
	 */
	A2(3, "A2"), //

	/**
	 * State B1.
	 * 
	 * <p>
	 * Car: Vehicle plugged without charging request, Wallbox: Wallbox doesn't allow
	 * charging
	 */
	B1(4, "B1"), //

	/**
	 * State B2.
	 * 
	 * <p>
	 * Car: Vehicle plugged without charging request, Wallbox: Wallbox allows
	 * charging
	 */
	B2(5, "B2"), //

	/**
	 * State C1.
	 * 
	 * <p>
	 * Car: Vehicle plugged with charging request, Wallbox: Wallbox doesn't allow
	 * charging
	 */
	C1(6, "C1"), //

	/**
	 * State C2.
	 * 
	 * <p>
	 * Car: Vehicle plugged with charging request, Wallbox: Wallbox allows charging
	 */
	C2(7, "C2"), //

	/**
	 * [State D].
	 * 
	 * <p>
	 * Car: Vehicle plugged with charging request, Wallbox: Derating
	 */
	D(8, "Derating"), //

	/**
	 * State E.
	 * 
	 * <p>
	 * Car: Error Wallbox: Error
	 */
	E(9, "E"), //

	/**
	 * State F.
	 * 
	 * <p>
	 * Car: Error, Wallbox: Wallbox locked or not ready
	 */
	F(10, "F"), //

	/**
	 * [State F2].
	 * 
	 * <p>
	 * Car: Error, Wallbox: Error
	 */
	F2(11, "Car Error") //
	;

	private final int value;
	private final String name;

	private HeidelbergStates(int value, String name) {
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
}
