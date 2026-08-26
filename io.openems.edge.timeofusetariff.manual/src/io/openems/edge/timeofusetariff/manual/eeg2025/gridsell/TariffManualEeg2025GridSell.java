package io.openems.edge.timeofusetariff.manual.eeg2025.gridsell;

import io.openems.common.channel.Level;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.timeofusetariff.api.TariffGridSell;

public interface TariffManualEeg2025GridSell extends OpenemsComponent, TariffGridSell {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		HTTP_STATUS_CODE(Doc.of(OpenemsType.INTEGER)//
				.text("Displays the HTTP status code")), //
		UNABLE_TO_FETCH_MARKET_PRICES(Doc.of(Level.WARNING)//
				.text("Unable to fetch market prices from ENTSO-E API")), //
		;

		private final Doc doc;

		ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}
}
