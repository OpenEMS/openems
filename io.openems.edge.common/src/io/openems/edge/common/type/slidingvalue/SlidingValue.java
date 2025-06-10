package io.openems.edge.common.type.slidingvalue;

import java.util.Objects;
import java.util.Optional;

import com.google.gson.JsonElement;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.type.TypeUtils;

/**
 * Calculates the 'Sliding Value' for a value.
 *
 * <p>
 * Using the existing subclasses, this class can be used to
 * <ul>
 * <li>calculate the average of several numeric values
 * <li>use the latest value for Enum and Boolean values
 * </ul>
 *
 * @param <T> the type of the Value
 */
public abstract class SlidingValue<T> {

	private final OpenemsType type;

	private T lastSentValue = null;

	protected SlidingValue(OpenemsType type) {
		this.type = type;
	}

	/**
	 * Adds a value.
	 *
	 * @param value the value
	 */
	public abstract void addValue(T value);

	/**
	 * Gets the sliding value, e.g. the average of all values.
	 *
	 * @return the sliding value
	 */
	protected abstract Optional<T> getSlidingValue();

	/**
	 * Resets the values.
	 */
	protected abstract void resetValues();

	/**
	 * Gets the OpenemsType of this SlidingValue.
	 *
	 * @return the OpenemsType
	 */
	protected OpenemsType getType() {
		return this.type;
	}

	/**
	 * Gets the value as a JsonElement if it changed. Resets the values.
	 *
	 * @return the value; or null if it had not changed
	 */
	public JsonElement getChangedValueOrNull() {
		var value = this.getSlidingValue().orElse(null);
		this.resetValues();
		if (Objects.equals(this.lastSentValue, value)) {
			return null;
		}
		this.lastSentValue = value;
		return TypeUtils.getAsJson(this.getType(), value);
	}

	/**
	 * Gets the value as a JsonElement. Resets the values.
	 *
	 * @return the value; null if is null
	 */
	public JsonElement getValue() {
		var value = this.getSlidingValue().orElse(null);
		this.resetValues();
		return TypeUtils.getAsJson(this.getType(), value);
	}

}