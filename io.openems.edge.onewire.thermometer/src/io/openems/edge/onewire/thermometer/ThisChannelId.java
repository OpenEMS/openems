package io.openems.edge.onewire.thermometer;

import io.openems.common.channel.Level;
import io.openems.edge.common.channel.Doc;

public enum ThisChannelId implements io.openems.edge.common.channel.ChannelId {
	COMMUNICATION_FAILED(Doc.of(Level.FAULT));

	private final Doc doc;

	private ThisChannelId(Doc doc) {
		this.doc = doc;
	}

	@Override
	public Doc doc() {
		return this.doc;
	}
}