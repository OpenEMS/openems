package io.openems.edge.kaco.blueplanet.hybrid10.ess;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.common.channel.Unit;

public enum ThisChannelId implements io.openems.edge.common.channel.ChannelId {
	BMS_VOLTAGE(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT)), //
	RISO(Doc.of(OpenemsType.FLOAT).unit(Unit.OHM)), //
	SURPLUS_FEED_IN(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.WATT));

	private final Doc doc;

	private ThisChannelId(Doc doc) {
		this.doc = doc;
	}

	@Override
	public Doc doc() {
		return this.doc;
	}
}