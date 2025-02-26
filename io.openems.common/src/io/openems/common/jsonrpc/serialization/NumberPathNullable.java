package io.openems.common.jsonrpc.serialization;

import java.util.Optional;

public interface NumberPathNullable extends JsonPath {

	/**
	 * Checks if the current value is present.
	 * 
	 * @return true if the current value is present; else false
	 */
	public boolean isPresent();

	/**
	 * Gets the string value of the current path.
	 * 
	 * @return the value
	 */
	public Number getOrNull();

	/**
	 * Gets the current value as a double or returns the provided default value if
	 * the current element is not present.
	 * 
	 * @param defaultValue the default value to provide in case the current value is
	 *                     not present
	 * @return the current value as a double or the default value if the current
	 *         value is not present
	 */
	public default double getAsDoubleOrDefault(double defaultValue) {
		return this.isPresent() ? this.getOrNull().doubleValue() : defaultValue;
	}

	/**
	 * Gets the current value as a float or returns the provided default value if
	 * the current element is not present.
	 * 
	 * @param defaultValue the default value to provide in case the current value is
	 *                     not present
	 * @return the current value as a float or the default value if the current
	 *         value is not present
	 */
	public default float getAsFloatOrDefault(float defaultValue) {
		return this.isPresent() ? this.getOrNull().floatValue() : defaultValue;
	}

	/**
	 * Gets the current value as a long or returns the provided default value if the
	 * current element is not present.
	 * 
	 * @param defaultValue the default value to provide in case the current value is
	 *                     not present
	 * @return the current value as a long or the default value if the current value
	 *         is not present
	 */
	public default long getAsLongOrDefault(long defaultValue) {
		return this.isPresent() ? this.getOrNull().longValue() : defaultValue;
	}

	/**
	 * Gets the current value as a integer or returns the provided default value if
	 * the current element is not present.
	 * 
	 * @param defaultValue the default value to provide in case the current value is
	 *                     not present
	 * @return the current value as a integer or the default value if the current
	 *         value is not present
	 */
	public default int getAsIntOrDefault(int defaultValue) {
		return this.isPresent() ? this.getOrNull().intValue() : defaultValue;
	}

	/**
	 * Gets the current value as a short or returns the provided default value if
	 * the current element is not present.
	 * 
	 * @param defaultValue the default value to provide in case the current value is
	 *                     not present
	 * @return the current value as a short or the default value if the current
	 *         value is not present
	 */
	public default short getAsShortOrDefault(short defaultValue) {
		return this.isPresent() ? this.getOrNull().shortValue() : defaultValue;
	}

	/**
	 * Gets the current value as a byte or returns the provided default value if the
	 * current element is not present.
	 * 
	 * @param defaultValue the default value to provide in case the current value is
	 *                     not present
	 * @return the current value as a byte or the default value if the current value
	 *         is not present
	 */
	public default byte getAsByteOrDefault(byte defaultValue) {
		return this.isPresent() ? this.getOrNull().byteValue() : defaultValue;
	}

	/**
	 * Gets the current value as a {@link Optional} of double.
	 * 
	 * @return the current value as a {@link Optional} of double
	 */
	public default Optional<Double> getAsOptionalDouble() {
		return Optional.ofNullable(this.getOrNull()) //
				.map(Number::doubleValue);
	}

	/**
	 * Gets the current value as a {@link Optional} of float.
	 * 
	 * @return the current value as a {@link Optional} of float
	 */
	public default Optional<Float> getAsOptionalFloat() {
		return Optional.ofNullable(this.getOrNull()) //
				.map(Number::floatValue);
	}

	/**
	 * Gets the current value as a {@link Optional} of long.
	 * 
	 * @return the current value as a {@link Optional} of long
	 */
	public default Optional<Long> getAsOptionalLong() {
		return Optional.ofNullable(this.getOrNull()) //
				.map(Number::longValue);
	}

	/**
	 * Gets the current value as a {@link Optional} of integer.
	 * 
	 * @return the current value as a {@link Optional} of integer
	 */
	public default Optional<Integer> getAsOptionalInt() {
		return Optional.ofNullable(this.getOrNull()) //
				.map(Number::intValue);
	}

	/**
	 * Gets the current value as a {@link Optional} of short.
	 * 
	 * @return the current value as a {@link Optional} of short
	 */
	public default Optional<Short> getAsOptionalShort() {
		return Optional.ofNullable(this.getOrNull()) //
				.map(Number::shortValue);
	}

	/**
	 * Gets the current value as a {@link Optional} of byte.
	 * 
	 * @return the current value as a {@link Optional} of byte
	 */
	public default Optional<Byte> getAsOptionalByte() {
		return Optional.ofNullable(this.getOrNull()) //
				.map(Number::byteValue);
	}

}
