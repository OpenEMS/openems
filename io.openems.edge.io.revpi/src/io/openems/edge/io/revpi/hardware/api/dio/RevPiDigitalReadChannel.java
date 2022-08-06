package io.openems.edge.io.revpi.hardware.api.dio;

import io.openems.edge.common.channel.BooleanReadChannel;

public class RevPiDigitalReadChannel {
	// Used only for mocking
	public RevPiDigitalReadChannel() {
	}

	public RevPiDigitalReadChannel(BooleanReadChannel channel, int nameIdx, String prefix) {
		Channel = channel;
		NameIdx = nameIdx;
		NamePrefix = prefix;
	}

	public BooleanReadChannel getReadChannel() {
		return Channel;
	}

	public String getChannelName() {
		return NamePrefix + NameIdx;
	}

	private BooleanReadChannel Channel;
	private int NameIdx;
	private String NamePrefix;
}
