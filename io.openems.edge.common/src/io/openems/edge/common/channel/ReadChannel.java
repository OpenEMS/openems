package io.openems.edge.common.channel;

public class ReadChannel implements Channel {

	private final ChannelDoc channelDoc;

	public ReadChannel(ChannelDoc channelDoc) {
		this.channelDoc = channelDoc;
	}

	@Override
	public ChannelDoc getChannelDoc() {
		return this.channelDoc;
	}

	@Override
	public void nextProcessImage() {
		System.out.println("nextProcessImage " + channelDoc);
	}

}
