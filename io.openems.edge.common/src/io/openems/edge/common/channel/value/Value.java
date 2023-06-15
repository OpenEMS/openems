package io.openems.edge.common.channel.value;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.Consumer;

import com.google.gson.JsonElement;

import io.openems.common.exceptions.InvalidValueException;
import io.openems.common.types.OpenemsType;
import io.openems.common.types.OptionsEnum;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.EnumDoc;
import io.openems.edge.common.type.TypeUtils;

/**
 * It is important to understand, that a Channel Value could be UNDEFINED (i.e.
 * 'null') at any point in time - e.g. due to lost communication connection to a
 * Device or Service, or because the system is just starting up and does not yet
 * have any data. This class wraps a 'value' information for a Channel and
 * provides convenience methods for retrieving it.
 *
 * <p>
 * To get the actual value of a Channel using this object, typically one of the
 * following methods will fit:
 * <ul>
 * <li>{@link #get()}: gets the value or null. Be aware of
 * {@link NullPointerException}s!
 * <li>{@link #getOrError()}: gets the value or throws an
 * {@link InvalidValueException} if the value is null
 * <li>{@link #orElse(Object)}: gets the value; or fallback alternative if the
 * value is null
 * </ul>
 *
 * @param <T> the type of the value
 */
public class Value<T> {

	/**
	 * The String for UNDEFINED, i.e. 'null' value channels.
	 */
	public static final String UNDEFINED_VALUE_STRING = "UNDEFINED";

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
	 * @return the value as formatted String
	 */
	public String asString() {
		return this.toString();
	}

	@Override
	public String toString() {
		if (this.value != null) {
			var optionString = this.asOptionString();
			return this.parent.channelDoc().getUnit().format(this.value, this.parent.getType())
					+ (optionString.isEmpty() ? "" : ":" + optionString);
		}
		var enumDoc = this.isEnumValue();
		if (enumDoc != null) {
			// special handling for EnumDocs
			return enumDoc.getOptionString(null);
		} else {
			return UNDEFINED_VALUE_STRING;
		}
	}

	/**
	 * Gets the value as a formatted String without its unit.
	 *
	 * @return formatted string without unit
	 */
	public String asStringWithoutUnit() {
		var value = this.get();
		if (value == null) {
			return Value.UNDEFINED_VALUE_STRING;
		}
		return value.toString();
	}

	/**
	 * Gets the value or null.
	 *
	 * @return the value
	 */
	public T get() {
		return this.value;
	}

	/**
	 * Gets the value or throws an Exception on null.
	 *
	 * @return the value; never null
	 * @throws InvalidValueException if value is null
	 */
	public T getOrError() throws InvalidValueException {
		var value = this.get();
		if (value != null) {
			return value;
		}
		throw new InvalidValueException("Value for Channel [" + this.parent.address() + "] is invalid.");
	}

	/**
	 * Gets the value as an Optional.
	 *
	 * @return the value as {@link Optional}
	 */
	public Optional<T> asOptional() {
		return Optional.ofNullable(this.get());
	}

	/**
	 * Is the value defined?. This is an abbreviation for
	 * Value.asOptional().isPresent().
	 *
	 * @return true if the value is defined; false if it is UNDEFINED
	 */
	public boolean isDefined() {
		return this.asOptional().isPresent();
	}

	/**
	 * If a value is present, performs the given action with the value, otherwise
	 * does nothing.
	 *
	 * @param action the action to be performed, if a value is present
	 * @throws NullPointerException if value is present and the given action is
	 *                              {@code null}
	 */
	public void ifPresent(Consumer<? super T> action) {
		this.asOptional().ifPresent(action);
	}

	/**
	 * Gets the value or the given alternativeValue. This is short for
	 * '.asOptional().or()'.
	 *
	 * @param alternativeValue the alternative value
	 * @return the value; or an alternative value if the value is null
	 */
	public T orElse(T alternativeValue) {
		return Optional.ofNullable(this.get()).orElse(alternativeValue);
	}

	/**
	 * Gets the value as its String option. Enum options are converted to Strings.
	 *
	 * @return the String option of Enum values
	 * @throws IllegalArgumentException no matching option existing
	 */
	public String asOptionString() {
		var enumDoc = this.isEnumValue();
		if (enumDoc == null) {
			return "";
		}
		var value = this.get();
		try {
			var intValue = TypeUtils.<Integer>getAsType(OpenemsType.INTEGER, value);
			return enumDoc.getOptionString(intValue);
		} catch (IllegalArgumentException e) {
			return enumDoc.getOptionString(null);
		}
	}

	/**
	 * Gets the value as its Enum option.
	 * 
	 * @param <O> the {@link OptionsEnum} type
	 * @return the Enum value
	 * @throws IllegalArgumentException no matching Enum option existing
	 */
	@SuppressWarnings("unchecked")
	public <O extends OptionsEnum> O asEnum() {
		var enumDoc = this.isEnumValue();
		if (enumDoc == null) {
			return null;
		}
		var value = this.get();
		if (value == null) {
			return (O) enumDoc.getOption(null);
		}
		try {
			int intValue = TypeUtils.<Integer>getAsType(OpenemsType.INTEGER, value);
			return (O) enumDoc.getOption(intValue);
		} catch (Exception e) {
			return (O) enumDoc.getOption(null);
		}
	}

	/**
	 * Gets the value in GSON JSON format.
	 *
	 * @return the value as {@link JsonElement}
	 */
	public JsonElement asJson() {
		return TypeUtils.getAsJson(this.parent.getType(), this.get());
	}

	/**
	 * Internal helper to find out if this Value refers to an EnumValue.
	 *
	 * @return the corresponding {@link EnumDoc}; or null if this Value is not an
	 *         enum
	 */
	private EnumDoc isEnumValue() {
		if (this.parent.channelDoc() instanceof EnumDoc) {
			return (EnumDoc) this.parent.channelDoc();
		}
		return null;
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
