package io.openems.edge.controller.io.heatingelement;

import static io.openems.common.channel.PersistencePriority.HIGH;
import static io.openems.common.channel.Unit.CUMULATED_SECONDS;
import static io.openems.common.channel.Unit.SECONDS;
import static io.openems.common.types.OpenemsType.INTEGER;
import static io.openems.common.types.OpenemsType.LONG;

import io.openems.edge.common.channel.Doc;
import io.openems.edge.controller.io.heatingelement.enums.Level;
import io.openems.edge.controller.io.heatingelement.enums.Status;

public interface ControllerIoHeatingElement {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		LEVEL(Doc.of(Level.values()) //
				.text("Current Level")),
		AWAITING_HYSTERESIS(Doc.of(INTEGER)), //
		PHASE1_TIME(Doc.of(INTEGER)//
				.unit(SECONDS)), //
		PHASE2_TIME(Doc.of(INTEGER)//
				.unit(SECONDS)), //
		PHASE3_TIME(Doc.of(INTEGER)//
				.unit(SECONDS)), //
		/*
		 * LEVELx_TIME was used for old history view. It is left for the analysis of the
		 * forced duration on a day.
		 */
		LEVEL1_TIME(Doc.of(INTEGER)//
				.unit(SECONDS)), //
		LEVEL2_TIME(Doc.of(INTEGER)//
				.unit(SECONDS)), //
		LEVEL3_TIME(Doc.of(INTEGER)//
				.unit(SECONDS)), //

		/*
		 * Total active Time of each Level.
		 */
		LEVEL1_CUMULATED_TIME(Doc.of(LONG)//
				.unit(CUMULATED_SECONDS) //
				.persistencePriority(HIGH)), //
		LEVEL2_CUMULATED_TIME(Doc.of(LONG)//
				.unit(CUMULATED_SECONDS) //
				.persistencePriority(HIGH)), //
		LEVEL3_CUMULATED_TIME(Doc.of(LONG)//
				.unit(CUMULATED_SECONDS) //
				.persistencePriority(HIGH)), //
		TOTAL_PHASE_TIME(Doc.of(INTEGER)//
				.unit(SECONDS)), //
		FORCE_START_AT_SECONDS_OF_DAY(Doc.of(INTEGER)//
				.unit(SECONDS)),
		STATUS(Doc.of(Status.values())); //

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