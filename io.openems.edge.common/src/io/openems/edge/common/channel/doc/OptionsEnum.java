package io.openems.edge.common.channel.doc;

public interface OptionsEnum {

	/**
	 * Gets this enums int representation.
	 * 
	 * @return
	 */
	int getValue();

	/**
	 * Gets this enums String representation.
	 * 
	 * @return
	 */
	String getName();

	/**
	 * Gets the enum that is used for 'UNDEFINED' values
	 * 
	 * @return
	 */
	OptionsEnum getUndefined();

	/**
	 * Gets whether the current enum represents the 'UNDEFINED' value
	 * 
	 * @return
	 */
	public default boolean isUndefined() {
		return this.equals(this.getUndefined());
	}
}
