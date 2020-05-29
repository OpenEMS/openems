package io.openems.edge.controller.io.heatingelement;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;

public interface ControllerHeatingElement {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		LEVEL(Doc.of(Level.values()) //
				.text("Current Level")),
		AWAITING_HYSTERESIS(Doc.of(OpenemsType.INTEGER)), //
		PHASE1_TIME(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.SECONDS)), //
		PHASE2_TIME(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.SECONDS)), //
		PHASE3_TIME(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.SECONDS)), //
		LEVEL1_TIME(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.SECONDS)), //
		LEVEL2_TIME(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.SECONDS)), //
		LEVEL3_TIME(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.SECONDS)), //
		TOTAL_PHASE_TIME(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.SECONDS)), //
		FORCE_START_AT_SECONDS_OF_DAY(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.SECONDS)); //

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

}