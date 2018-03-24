package io.openems.edge.common.channel;

public interface Channel {

	ChannelDoc getChannelDoc();

	/**
	 * Switches to the next process image, i.e. copies the "next"-value into
	 * "current"-value.
	 */
	void nextProcessImage();
}
