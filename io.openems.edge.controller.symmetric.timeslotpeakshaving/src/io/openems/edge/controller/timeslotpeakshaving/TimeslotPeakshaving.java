package io.openems.edge.controller.timeslotpeakshaving;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;

public interface TimeslotPeakshaving extends Controller, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		STATE_MACHINE(Doc.of(ChargeState.values()) //
				.text("Current State of State-Machine")),
		CALCULATED_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)),
		PEAK_SHAVED_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT));

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