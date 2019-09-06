package io.openems.edge.controller.ess.limitdischargecellvoltage;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;

public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
	STATE_MACHINE(Doc.of(State.values()).text("Current State of State-Machine")), //
	MIN_CELL_VOLTAGE(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIVOLT)),
	ESS_POWER(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT)),;

	private final Doc doc;

	private ChannelId(Doc doc) {
		this.doc = doc;
	}

	@Override
	public Doc doc() {
		return this.doc;
	}
}