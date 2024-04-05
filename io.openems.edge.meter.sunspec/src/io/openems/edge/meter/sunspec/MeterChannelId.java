package io.openems.edge.meter.sunspec;

import io.openems.edge.common.channel.Doc;

public enum MeterChannelId implements io.openems.edge.common.channel.ChannelId {
	;

	private final Doc doc;

	private MeterChannelId(Doc doc) {
		this.doc = doc;
	}

	@Override
	public Doc doc() {
		return this.doc;
	}
}