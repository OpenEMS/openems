package io.openems.edge.batteryinverter.kaco.blueplanetgridsave;

import io.openems.edge.common.channel.Doc;

public enum MyChannelId implements io.openems.edge.common.channel.ChannelId {
	;

	private final Doc doc;

	private MyChannelId(Doc doc) {
		this.doc = doc;
	}

	@Override
	public Doc doc() {
		return this.doc;
	}
}