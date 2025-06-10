package io.openems.common.jsonrpc.serialization;

import java.util.Optional;

public interface BooleanPathNullable {

	/**
	 * Checks if the current value is present.
	 * 
	 * @return true if the current value is present; else false
	 */
	public boolean isPresent();

	/**
	 * Gets the boolean value of the current path.
	 * 
	 * @return the value; or null if not present
	 */
	public Boolean getOrNull();

	/**
	 * Gets the current value if present otherwise returns the provided default
	 * value.
	 * 
	 * @param defaultValue the default value to provide if the current values is not
	 *                     present
	 * @return the current value if present; else the default value
	 */
	public default boolean getOrDefault(boolean defaultValue) {
		return this.isPresent() ? this.getOrNull() : defaultValue;
	}

	/**
	 * Gets the current value as a {@link Optional}.
	 * 
	 * @return a {@link Optional} of the current value
	 */
	public default Optional<Boolean> getOptional() {
		return Optional.ofNullable(this.getOrNull());
	}

}
