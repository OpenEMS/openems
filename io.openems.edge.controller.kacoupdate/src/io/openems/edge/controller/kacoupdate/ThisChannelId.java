package io.openems.edge.controller.kacoupdate;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.common.channel.Unit;

public enum ThisChannelId implements io.openems.edge.common.channel.ChannelId {
	HAS_EDGE_UPDATE(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE)),
	HAS_UI_UPDATE(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE));
	private final Doc doc;

	private ThisChannelId(Doc doc) {
		this.doc = doc;
	}

	@Override
	public Doc doc() {
		return this.doc;
	}
}