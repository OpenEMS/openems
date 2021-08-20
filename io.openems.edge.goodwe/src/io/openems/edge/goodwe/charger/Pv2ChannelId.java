package io.openems.edge.goodwe.charger;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.channel.Doc;

public enum Pv2ChannelId implements ChannelId, PvChannelId {

	V(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)),
	I(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY));

	private final Doc doc;

	private Pv2ChannelId(Doc doc) {
		this.doc = doc;
	}

	@Override
	public Doc doc() {
		return this.doc;
	}

	@Override
	public ChannelId getV() {
		return Pv2ChannelId.V;
	}

	@Override
	public ChannelId getI() {
		return Pv2ChannelId.I;
	}
}