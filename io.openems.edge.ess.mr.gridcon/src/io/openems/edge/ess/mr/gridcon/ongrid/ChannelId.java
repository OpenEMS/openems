package io.openems.edge.ess.mr.gridcon.ongrid;

import io.openems.edge.common.channel.Doc;

public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
	STATE_MACHINE(Doc.of(State.values()).text("Current State of State-Machine")), //
	
	;

	private final Doc doc;

	private ChannelId(Doc doc) {
		this.doc = doc;
	}

	@Override
	public Doc doc() {
		return this.doc;
	}
}