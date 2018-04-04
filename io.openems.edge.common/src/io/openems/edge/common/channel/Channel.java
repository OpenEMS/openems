package io.openems.edge.common.channel;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.doc.Doc;

public interface Channel {

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
	 *            Object needs to be converted internally to the correct format
	 */
	void setNextValue(Object value) throws OpenemsException;
}
