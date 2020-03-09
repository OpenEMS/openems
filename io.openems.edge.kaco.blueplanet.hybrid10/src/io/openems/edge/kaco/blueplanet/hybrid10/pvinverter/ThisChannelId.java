package io.openems.edge.kaco.blueplanet.hybrid10.pvinverter;

import io.openems.common.channel.Level;
import io.openems.edge.common.channel.Doc;

public enum ThisChannelId implements io.openems.edge.common.channel.ChannelId {
	PV_LIMIT_FAILED(Doc.of(Level.FAULT) //
			.text("PV-Limit failed"));
	;
	private final Doc doc;

	private ThisChannelId(Doc doc) {
		this.doc = doc;
	}

	@Override
	public Doc doc() {
		return this.doc;
	}
}