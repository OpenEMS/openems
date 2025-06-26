package io.openems.common.channel;

public enum Debounce {
	/**
	 * Debounce-Setting: If the StateChannel value is continuously set to 'true' for
	 * configured times in a row, the value of the StateChannel is set to true;
	 * otherwise false.
	 */
	TRUE_VALUES_IN_A_ROW_TO_SET_TRUE,
	/**
	 * Debounce-Setting: If the StateChannel value is continuously set to 'false'
	 * for configured times in a row, the value of the StateChannel is set to false;
	 * otherwise true.
	 */
	FALSE_VALUES_IN_A_ROW_TO_SET_FALSE,
	/**
	 * Debounce-Setting: If the StateChannel value is continuously set to the same
	 * value for configured times in a row, the value of the StateChannel is set to
	 * that value; otherwise stays at the old value.
	 */
	SAME_VALUES_IN_A_ROW_TO_CHANGE
}
