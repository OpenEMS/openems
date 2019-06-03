package io.openems.edge.meter.spanner.bhkw;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;

public enum BHKWChannelId implements io.openems.edge.common.channel.ChannelId {
	// EnumReadChannels
	FREQUENCY_L1(Doc.of(OpenemsType.INTEGER)//
			.unit(Unit.MILLIHERTZ)), //
	FREQUENCY_L2(Doc.of(OpenemsType.INTEGER)//
			.unit(Unit.MILLIHERTZ)), //
	FREQUENCY_L3(Doc.of(OpenemsType.INTEGER)//
			.unit(Unit.MILLIHERTZ)), //
	COSPHI_L1(Doc.of(OpenemsType.INTEGER)), //
	COSPHI_L2(Doc.of(OpenemsType.INTEGER)), //
	COSPHI_L3(Doc.of(OpenemsType.INTEGER)) //
	; //

	private final Doc doc;

	private BHKWChannelId(Doc doc) {
		this.doc = doc;
	}

	@Override
	public Doc doc() {
		return this.doc;
	}
}