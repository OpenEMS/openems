package io.openems.edge.common.channel;

import java.util.Optional;
import java.util.function.Consumer;

import io.openems.common.exceptions.InvalidValueException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.common.utils.TypeUtils;
import io.openems.edge.common.channel.doc.Doc;

public interface Channel<T> {

	public final static String UNDEFINED_VALUE_STRING = "UNDEFINED";

	/**
	 * Gets the ChannelId of this Channel
	 * 
	 * @return
	 */
	io.openems.edge.common.channel.doc.ChannelId channelId();

	/**
	 * Gets the ChannelDoc of this Channel
	 * 
	 * @return
	 */
	default Doc channelDoc() {
		return this.channelId().doc();
	}

	/**
	 * Gets the address of this Channel
	 * 
	 * @return
	 */
	ChannelAddress address();

	/**
	 * Switches to the next process image, i.e. copies the "next"-value into
	 * "current"-value.
	 */
	void nextProcessImage();

	/**
	 * Gets the type of this Channel, e.g. INTEGER, BOOLEAN,..
	 * 
	 * @return
	 */
	OpenemsType getType();

	/**
	 * Updates the 'next' value of Channel.
	 * 
	 * @param value
	 */
	public default void setNextValue(Object value) throws OpenemsException {
		this._setNextValue(TypeUtils.<T>getAsType(this.getType(), value));
	}

	/**
	 * Internal method. Do not call directly.
	 * 
	 * @param value
	 */
	@Deprecated
	public void _setNextValue(T value);

	/**
	 * Gets the currently active value or Null
	 * 
	 * @return
	 */
	T getActiveValueOrNull();

	/**
	 * Gets the currently active value or throws an Exception on Null
	 * 
	 * @return
	 */
	default T getActiveValue() throws InvalidValueException {
		T value = this.getActiveValueOrNull();
		if (value != null) {
			return value;
		}
		throw new InvalidValueException("Value for Channel [" + this.address() + "] is invalid.");
	}

	/**
	 * Gets the currently active value as an Optional.
	 * 
	 * @return
	 */
	default Optional<T> getActiveValueOpt() {
		return Optional.ofNullable(this.getActiveValueOrNull());
	};

	/**
	 * Gets the currently active value as its String option. Enum options are
	 * converted to Strings.
	 * 
	 * @throws IllegalArgumentException
	 *             no matching option was provided
	 * @return
	 */
	default String getActiveValueOption() throws IllegalArgumentException {
		Optional<T> valueOpt = this.getActiveValueOpt();
		if (!valueOpt.isPresent()) {
			return Channel.UNDEFINED_VALUE_STRING;
		}
		int value = TypeUtils.<Integer>getAsType(OpenemsType.INTEGER, valueOpt.get());
		return this.channelDoc().getOption(value);
	}

	/**
	 * Gets the currently active value as its Enum option.
	 * 
	 * @throws IllegalArgumentException
	 *             no matching Enum option was provided
	 * @return
	 * @throws InvalidValueException
	 */
	default Enum<?> getActiveValueOptionEnum() throws InvalidValueException {
		T valueObj = this.getActiveValue();
		int value = TypeUtils.<Integer>getAsType(OpenemsType.INTEGER, valueObj);
		return this.channelDoc().getOptionEnum(value);
	}

	/**
	 * Formats the Channel. Can be used like toString()
	 * 
	 * @return
	 */
	default String format() {
		return this.channelDoc().getUnit().format(this.formatWithoutUnit(), this.getType());
	}

	/**
	 * Formats the Channel. Can be used like toString()
	 * 
	 * @return
	 */
	default String formatWithoutUnit() {
		Object value = this.getActiveValueOrNull();
		if (value == null) {
			return Channel.UNDEFINED_VALUE_STRING;
		}
		return value.toString();
	}

	/**
	 * Add an onUpdate callback. It is called, after a new ActiveValue was set via
	 * nextProcessImage().
	 */
	public void onUpdate(Consumer<T> callback);
}
