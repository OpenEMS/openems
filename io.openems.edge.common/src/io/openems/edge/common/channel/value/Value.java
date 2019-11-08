package io.openems.edge.common.channel.value;

import java.time.LocalDateTime;
import java.util.Optional;

import com.google.gson.JsonElement;

import io.openems.common.exceptions.InvalidValueException;
import io.openems.common.types.OpenemsType;
import io.openems.common.types.OptionsEnum;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.EnumDoc;
import io.openems.edge.common.type.TypeUtils;

/**
 * This wraps a 'value' information for a Channel and provides convenience
 * methods for retrieving it.
 * 
 * @param <T> the type of the value
 */
public class Value<T> {

	public final static String UNDEFINED_VALUE_STRING = "UNDEFINED";

	private final Channel<T> parent;
	private final T value;
	private final LocalDateTime timestamp;

	public Value(Channel<T> parent, T value) {
		this.parent = parent;
		this.value = value;
		this.timestamp = LocalDateTime.now();
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
			EnumDoc enumDoc = this.isEnumValue();
			if (enumDoc != null) {
				// special handling for EnumDocs
				return enumDoc.getOptionString(null);
			} else {
				return UNDEFINED_VALUE_STRING;
			}
		} else {
			String optionString = this.asOptionString();
			return this.parent.channelDoc().getUnit().format(this.value, this.parent.getType())
					+ (optionString.isEmpty() ? "" : ":" + optionString);
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
	 * Is the value defined?. This is an abbreviation for
	 * Value.asOptional().isPresent().
	 *
	 * @return true if the value is defined; false if it is UNDEFINED
	 */
	public boolean isDefined() {
		return this.asOptional().isPresent();
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
	public String asOptionString() {
		EnumDoc enumDoc = this.isEnumValue();
		if (enumDoc != null) {
			T value = this.get();
			try {
				Integer intValue = TypeUtils.<Integer>getAsType(OpenemsType.INTEGER, value);
				return enumDoc.getOptionString(intValue);
			} catch (IllegalArgumentException e) {
				return enumDoc.getOptionString(null);
			}
		} else {
			return "";
		}
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
	 * Gets the value as its Enum option.
	 *
	 * @throws IllegalArgumentException no matching Enum option existing
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <O extends OptionsEnum> O asEnum() {
		EnumDoc enumDoc = this.isEnumValue();
		if (enumDoc != null) {
			T value = this.get();
			if (value == null) {
				return (O) enumDoc.getOption(null);
			}
			try {
				int intValue = TypeUtils.<Integer>getAsType(OpenemsType.INTEGER, value);
				return (O) enumDoc.getOption(intValue);
			} catch (Exception e) {
				return (O) enumDoc.getOption(null);
			}
		} else {
			return null;
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

	/**
	 * Internal helper to find out if this Value referrs to an EnumValue.
	 * 
	 * @return the corresponding {@link EnumDoc}; or null if this Value is not an
	 *         enum
	 */
	private EnumDoc isEnumValue() {
		if (this.parent.channelDoc() instanceof EnumDoc) {
			EnumDoc enumDoc = (EnumDoc) this.parent.channelDoc();
			return enumDoc;
		} else {
			return null;
		}
	}

	/**
	 * Gets the timestamp when the value was created.
	 * 
	 * @return the timestamp
	 */
	public LocalDateTime getTimestamp() {
		return this.timestamp;
	}
}
