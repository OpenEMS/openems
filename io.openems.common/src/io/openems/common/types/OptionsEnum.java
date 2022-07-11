package io.openems.common.types;

import com.google.common.base.CaseFormat;

public interface OptionsEnum {

	/**
	 * Gets this enums int representation.
	 *
	 * @return the int representation
	 */
	public int getValue();

	/**
	 * Gets this enums String representation.
	 *
	 * @return the String representation
	 */
	public String getName();

	/**
	 * Gets the enum that is used for 'UNDEFINED' values.
	 *
	 * @return the UNDEFINED enum
	 */
	public OptionsEnum getUndefined();

	/**
	 * Gets the name in CamelCase format.
	 *
	 * <p>
	 * If {@link #getName()} returns 'MY_VALUE' this method returns 'MyValue'.
	 *
	 * @return the Name in CamelCase format
	 */
	public default String asCamelCase() {
		return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, this.getName());
	}

	/**
	 * Gets whether the current enum represents the 'UNDEFINED' value.
	 *
	 * @return true if this is the UNDEFINED enum
	 */
	public default boolean isUndefined() {
		return this.equals(this.getUndefined());
	}
}
