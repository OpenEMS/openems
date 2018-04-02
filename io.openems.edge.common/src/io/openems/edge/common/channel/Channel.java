package io.openems.edge.common.channel;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.doc.ChannelDoc;

public interface Channel {

	/**
	 * Gets the ChannelDoc of this Channel
	 * 
	 * @return
	 */
	ChannelDoc channelDoc();

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
	 * Updates the 'next' value of Channel.
	 * 
	 * @param value
	 *            Object needs to be converted internally to the correct format
	 */
	void setNextValue(Object value) throws OpenemsException;
}
