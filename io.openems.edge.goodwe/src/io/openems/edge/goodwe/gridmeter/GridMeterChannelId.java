package io.openems.edge.goodwe.gridmeter;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.channel.Doc;

public enum GridMeterChannelId implements ChannelId {

	F_GRID_R(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.HERTZ) //
			.accessMode(AccessMode.READ_ONLY)),

	F_GRID_S(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.HERTZ) //
			.accessMode(AccessMode.READ_ONLY)),

	F_GRID_T(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.HERTZ) //
			.accessMode(AccessMode.READ_ONLY)),

	METER_POWER_FACTOR(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE) //
			.accessMode(AccessMode.READ_ONLY)); //

	private final Doc doc;

	private GridMeterChannelId(Doc doc) {
		this.doc = doc;
	}

	@Override
	public Doc doc() {
		return this.doc;
	}
}