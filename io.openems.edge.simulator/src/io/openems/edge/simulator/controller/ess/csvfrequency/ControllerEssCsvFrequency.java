package io.openems.edge.simulator.controller.ess.csvfrequency;

import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;

public interface ControllerEssCsvFrequency extends Controller, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		FREQUENCY(Doc.of(OpenemsType.DOUBLE)//
				.unit(Unit.HERTZ)//
				.persistencePriority(PersistencePriority.HIGH)//
				.text("Current Frequency Read from CSV")//
		),

		FCR_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)//
				.persistencePriority(PersistencePriority.HIGH)//
				.text("FCR Power Component")//
		),

		FFR1_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)//
				.persistencePriority(PersistencePriority.HIGH)//
				.text("FFR1 Power Component")//
		),

		RECHARGE_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)//
				.persistencePriority(PersistencePriority.HIGH)//
				.text("Recharge Power Component")//
		),

		CALCULATED_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)//
				.persistencePriority(PersistencePriority.HIGH)//
				.text("Calculated Target Power before ramp (FCR + FFR1 + Recharge)")//
		),

		TOTAL_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)//
				.persistencePriority(PersistencePriority.HIGH)//
				.text("Actual Ramped Power applied to ESS")//
		),;

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
