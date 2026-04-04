package io.openems.core.referencetarget;

public interface ValueProvider {

	/**
	 * Gets the value of the variable.
	 * 
	 * @param variable the variable to get the value from
	 * @return the value or null if not found
	 */
	Object getValue(String variable);

}
