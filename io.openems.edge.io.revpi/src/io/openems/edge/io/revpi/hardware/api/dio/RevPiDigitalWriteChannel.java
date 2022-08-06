package io.openems.edge.io.revpi.hardware.api.dio;

import io.openems.edge.common.channel.BooleanWriteChannel;

public class RevPiDigitalWriteChannel {
	public RevPiDigitalWriteChannel(BooleanWriteChannel channel, int nameIdx, String prefix) {
		Channel = channel;
		NameIdx = nameIdx;
		NamePrefix = prefix;
	}

	public BooleanWriteChannel getWriteChannel() {
		return Channel;
	}

	public String getChannelName() {
		return NamePrefix + NameIdx;
	}

	private BooleanWriteChannel Channel;
	private int NameIdx;
	private String NamePrefix;
}
