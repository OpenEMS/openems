package io.openems.edge.common.channel.value;

import java.util.Optional;

import com.google.gson.JsonElement;

import io.openems.common.exceptions.InvalidValueException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.type.TypeUtils;

/**
 * This wraps a 'value' information for a Channel and provides convenience
 * methods for retrieving it.
 * 
 * @param <T>
 */
public class Value<T> {

	public final static String UNDEFINED_VALUE_STRING = "UNDEFINED";

	private final Channel<T> parent;
	private final T value;

	public Value(Channel<T> parent, T value) {
		this.parent = parent;
		this.value = value;
	}

	/**
	 * Gets the value as a formatted String with its unit. (Same as toString())
	 * 
	 * @return
	 */
	public String asString() {
		return this.toString();
	}

	public String toString() {
		if (this.value == null) {
			return UNDEFINED_VALUE_STRING;
		} else {
			return this.parent.channelDoc().getUnit().format(this.value, this.parent.getType());
		}
	}

	/**
	 * Gets the value as a formatted String without its unit
	 *
	 * @return
	 */
	public String asStringWithoutUnit() {
		T value = this.get();
		if (value == null) {
			return Value.UNDEFINED_VALUE_STRING;
		}
		return value.toString();
	}

	/**
	 * Gets the value or null
	 *
	 * @return
	 */
	public T get() {
		return this.value;
	}

	/**
	 * Gets the value or throws an Exception on null
	 *
	 * @return
	 */
	public T getOrError() throws InvalidValueException {
		T value = this.get();
		if (value != null) {
			return value;
		}
		throw new InvalidValueException("Value for Channel [" + this.parent.address() + "] is invalid.");
	}

	/**
	 * Gets the value as an Optional.
	 *
	 * @return
	 */
	public Optional<T> asOptional() {
		return Optional.ofNullable(this.get());
	};

	/**
	 * Gets the value or the given alternativeValue. This is short for
	 * '.asOptional().or()'.
	 *
	 * @return
	 */
	public T orElse(T alternativeValue) {
		return Optional.ofNullable(this.get()).orElse(alternativeValue);
	};

	/**
	 * Gets the value as its String option. Enum options are converted to Strings.
	 *
	 * @throws IllegalArgumentException no matching option existing
	 * @return
	 */
	public String asOptionString() throws IllegalArgumentException {
		T value = this.get();
		if (value == null) {
			return Value.UNDEFINED_VALUE_STRING;
		}
		int intValue = TypeUtils.<Integer>getAsType(OpenemsType.INTEGER, value);
		return this.parent.channelDoc().getOption(intValue);
	}

	/**
	 * Gets the value as its Enum option.
	 *
	 * @throws IllegalArgumentException no matching Enum option existing
	 * @return
	 * @throws InvalidValueException
	 */
	public Enum<?> asEnum() throws InvalidValueException {
		T value = this.get();
		int intValue = TypeUtils.<Integer>getAsType(OpenemsType.INTEGER, value);
		return this.parent.channelDoc().getOptionEnum(intValue);
	}

	/**
	 * Gets the value as an Optional enum
	 * 
	 * @return
	 */
	public Optional<Enum<?>> asEnumOptional() {
		try {
			return Optional.ofNullable(asEnum());
		} catch (Exception e) { // if there is null in asEnum a NullPointerException is thrown
			return Optional.empty();
		}
	}

	/**
	 * Gets the value in GSON JSON format
	 * 
	 * @return
	 */
	public JsonElement asJson() {
		return TypeUtils.getAsJson(this.parent.getType(), this.get());
	}
}
