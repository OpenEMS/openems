package io.openems.edge.controller.ess.timeofusetariff.discharge;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;

public interface TimeOfUseTariffDischarge extends Controller, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		BUYING_FROM_GRID(Doc.of(OpenemsType.BOOLEAN)//
				.text("The controller selfoptimization ran succefully")),
		TARGET_HOURS_IS_EMPTY(Doc.of(OpenemsType.BOOLEAN)//
				.text("The list of target hours is empty")),
		QUATERLY_PRICES_TAKEN(Doc.of(OpenemsType.BOOLEAN)//
				.text("The controller retrieves hourly Prices from API successfully")),
		TARGET_HOURS_CALCULATED(Doc.of(OpenemsType.BOOLEAN)//
				.text("The controller calculates target time to buy from grid successfully")),
		TOTAL_CONSUMPTION(Doc.of(OpenemsType.INTEGER) //
				.text("Total consmption for the night")),
		QUARTERLY_PRICES(Doc.of(OpenemsType.FLOAT) //
				.text("Price of the electricity for the corresponding Hour.")),
		REMAINING_CONSUMPTION(Doc.of(OpenemsType.DOUBLE) //
				.text("remaining consmption to charge from grid")),
		NUMBER_OF_TARGET_HOURS(Doc.of(OpenemsType.INTEGER) //
				.text("Target Hours")),
		AVAILABLE_CAPACITY(Doc.of(OpenemsType.INTEGER) //
				.text("Available capcity in the battery during evening")), //
		PRO_MORE_THAN_CON(Doc.of(OpenemsType.INTEGER) //
				.text("Hour of Production more than Consumption")),
		PRO_LESS_THAN_CON(Doc.of(OpenemsType.INTEGER) //
				.text("Hour of Production less than Consumption")),
		PRODUCTION(Doc.of(OpenemsType.INTEGER) //
				.text("Production")),
		CONSUMPTON(Doc.of(OpenemsType.INTEGER) //
				.text("Consumption"));

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
