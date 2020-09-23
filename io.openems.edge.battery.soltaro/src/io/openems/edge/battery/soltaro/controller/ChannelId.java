package io.openems.edge.battery.soltaro.controller;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;

public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
	STATE_MACHINE(Doc.of(State.values()).text("Current State of State-Machine")), //
	MIN_CELL_VOLTAGE(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIVOLT)), //
	MAX_CELL_VOLTAGE(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIVOLT)), //
	MIN_CELL_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS)), //
	MAX_CELL_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS)), //
	ESS_SOC(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)), //
	ESS_POWER(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT)), //
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