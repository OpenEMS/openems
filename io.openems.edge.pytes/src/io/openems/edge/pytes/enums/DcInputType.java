package io.openems.edge.pytes.enums;

import io.openems.common.types.OptionsEnum;

// ToDo
public enum DcInputType implements OptionsEnum {
	UNDEFINED(-1, "Undefined"),

	/** 0 – Only 1 DC input string connected. */
	ONE_INPUT(0, "1 Input"),

	/** 1 – 2 DC input strings connected. */
	TWO_INPUTS(1, "2 Inputs"),

	/** 2 – 3 DC input strings connected. */
	THREE_INPUTS(2, "3 Inputs"),

	/** 3 – 4 DC input strings connected. */
	FOUR_INPUTS(3, "4 Inputs"),

	/** 4 – 5 DC input strings connected. */
	FIVE_INPUTS(4, "5 Inputs"),

	/** 5 – 6 DC input strings connected. */
	SIX_INPUTS(5, "6 Inputs"),

	/** 6 – 7 DC input strings connected. */
	SEVEN_INPUTS(6, "7 Inputs"),

	/** 7 – 8 DC input strings connected. */
	EIGHT_INPUTS(7, "8 Inputs");


	private final int value;
	private final String name;

	DcInputType(int value, String name) {
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
