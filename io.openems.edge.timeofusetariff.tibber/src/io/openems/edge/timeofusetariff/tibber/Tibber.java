package io.openems.edge.timeofusetariff.tibber;

import io.openems.common.channel.Level;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.timeofusetariff.api.TimeOfUseTariff;

public interface Tibber extends TimeOfUseTariff, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		HTTP_STATUS_CODE(Doc.of(OpenemsType.INTEGER) //
				.text("The HTTP status code")), //
		UNABLE_TO_UPDATE_PRICES(Doc.of(Level.WARNING) //
				.text("Unable to update prices from Tibber API")), //
		FILTER_IS_REQUIRED(Doc.of(Level.WARNING) //
				.text("Found multiple 'Homes'. Please configure either an ID (format UUID) "
						+ "or 'appNickname' for unambiguous identification")) //
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
}
