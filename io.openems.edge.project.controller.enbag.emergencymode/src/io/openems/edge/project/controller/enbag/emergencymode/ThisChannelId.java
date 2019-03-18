package io.openems.edge.project.controller.enbag.emergencymode;

import io.openems.edge.common.channel.doc.Doc;

public enum ThisChannelId implements io.openems.edge.common.channel.doc.ChannelId {
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