package io.openems.edge.common.channel;

import io.openems.edge.common.channel.doc.Doc;

public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
	TEST_CHANNEL_WITH_OPTIONS(new Doc().options(TestOptions.values()));

	private final Doc doc;

	private ChannelId(Doc doc) {
		this.doc = doc;
	}

	@Override
	public Doc doc() {
		return this.doc;
	}
}