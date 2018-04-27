package io.openems.edge.common.channel;

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
	 * Formats the Channel. Can be used like toString()
	 * 
	 * @return
	 */
	default String format() {
		return this.channelDoc().getUnit().format(this.getActiveValue(), this.getType());
	}
}
