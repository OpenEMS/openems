package io.openems.edge.common.channel;

import java.util.function.Consumer;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.common.utils.TypeUtils;
import io.openems.edge.common.channel.doc.Doc;

public interface Channel<T> {

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
	 * Gets the currently active value
	 * 
	 * @return
	 */
	T getActiveValue();

	/**
	 * Gets the currently active value as its String option. Enum options are
	 * converted to Strings.
	 * 
	 * @throws IllegalArgumentException
	 *             no matching option was provided
	 * @return
	 */
	default String getActiveValueOption() {
		T valueObj = this.getActiveValue();
		int value = TypeUtils.<Integer>getAsType(OpenemsType.INTEGER, valueObj);
		return this.channelDoc().getOption(value);
	}

	/**
	 * Gets the currently active value as its Enum option.
	 * 
	 * @throws IllegalArgumentException
	 *             no matching Enum option was provided
	 * @return
	 */
	default Enum<?> getActiveValueOptionEnum() {
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
		return this.channelDoc().getUnit().format(this.getActiveValue(), this.getType());
	}

	/**
	 * Add an onUpdate callback. It is called, after a new ActiveValue was set via
	 * nextProcessImage().
	 */
	public void onUpdate(Consumer<T> callback);
}
