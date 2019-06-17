package io.openems.edge.project.hofgutkarpfsee;

import io.openems.edge.common.channel.Doc;

public enum ThisChannelId implements io.openems.edge.common.channel.ChannelId {
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