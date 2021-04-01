package io.openems.edge.controller.dynamicdischarge;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;

public interface DynamicDischarge extends Controller, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		BUYING_FROM_GRID(Doc.of(OpenemsType.BOOLEAN)//
				.text("The controller selfoptimization ran succefully")),
		HOURLY_PRICES_TAKEN(Doc.of(OpenemsType.BOOLEAN)//
				.text("The controller retrieves hourly Prices from API successfully")),
		TARGET_HOURS_CALCULATED(Doc.of(OpenemsType.BOOLEAN)//
				.text("The controller calculated hours successfully")),
		TOTAL_CONSUMPTION(Doc.of(OpenemsType.INTEGER) //
				.text("Total consmption for the night")),
		REMAINING_CONSUMPTION(Doc.of(OpenemsType.INTEGER) //
				.text("remaining consmption to charge from grid")),
		NUMBER_OF_TARGET_HOURS(Doc.of(OpenemsType.INTEGER) //
				.text("Target Hours")),
		AVAILABLE_CAPACITY(Doc.of(OpenemsType.INTEGER) //
				.text("Available capcity in the battery during evening"));

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
