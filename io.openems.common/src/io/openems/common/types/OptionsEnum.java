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

	/**
	 * Gets the Option value from a value or null (not UNDEFINED!).
	 * 
	 * @param <T>       OptionsEnum
	 * @param enumClass the enum class
	 * @param value     the value of the Option
	 * @return the enum value or null
	 */
	public static <T extends Enum<T> & OptionsEnum> T getOption(Class<T> enumClass, int value) {
		for (var e : enumClass.getEnumConstants()) {
			if (e.getValue() == value) {
				return e;
			}
		}
		return null;
	}

	/**
	 * Gets the Option value from a value.
	 * 
	 * @param <T>       OptionsEnum
	 * @param enumClass the enum class
	 * @param value     the value of the Option
	 * @return the enum value or getUndefined
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Enum<T> & OptionsEnum> T getOptionOrUndefined(Class<T> enumClass, int value) {
		var enumConstants = enumClass.getEnumConstants();
		if (enumConstants.length == 0) {
			return null;
		}
		for (var e : enumConstants) {
			if (e.getValue() == value) {
				return e;
			}
		}

		if (enumClass.isInstance(enumConstants[0].getUndefined())) {
			// TODO: Refactor OptionsEnum to support <T>
			return (T) enumConstants[0].getUndefined();
		}
		return null;
	}
}
