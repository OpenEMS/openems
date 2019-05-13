package io.openems.common.types;

public interface OptionsEnum {

	/**
	 * Gets this enums int representation.
	 * 
	 * @return the int representation
	 */
	int getValue();

	/**
	 * Gets this enums String representation.
	 * 
	 * @return the String representation
	 */
	String getName();

	/**
	 * Gets the enum that is used for 'UNDEFINED' values.
	 * 
	 * @return the UNDEFINED enum
	 */
	OptionsEnum getUndefined();

	/**
	 * Gets whether the current enum represents the 'UNDEFINED' value.
	 * 
	 * @return true if this is the UNDEFINED enum
	 */
	public default boolean isUndefined() {
		return this.equals(this.getUndefined());
	}
}
