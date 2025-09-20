package io.openems.common.jsonrpc.serialization;

import java.util.Optional;

public interface StringPathNullable<T> extends JsonPath {

	/**
	 * Gets the raw string value of the current path.
	 * 
	 * @return the value
	 */
	public String getRawOrNull();

	/**
	 * Gets the parsed string value of the current path.
	 * 
	 * @return the value
	 */
	public T getOrNull();

	/**
	 * Gets the current raw value as a {@link Optional}.
	 * 
	 * @return a {@link Optional} of the current raw value
	 */
	public default Optional<String> getRawOptional() {
		return Optional.ofNullable(this.getRawOrNull());
	}

	/**
	 * Gets the current value as a {@link Optional}.
	 * 
	 * @return a {@link Optional} of the current value
	 */
	public default Optional<T> getOptional() {
		return Optional.ofNullable(this.getOrNull());
	}

}
