package io.openems.edge.timeofusetariff.spottariff.oldentsoe.gridsell;

import io.openems.common.channel.Level;
import io.openems.common.tariff.api.Tariff;
import io.openems.common.tariff.api.TariffProvider;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.timeofusetariff.api.TariffComponent;
import io.openems.edge.timeofusetariff.api.TariffGridSell;
import io.openems.edge.timeofusetariff.spottariff.oldentsoe.gridbuy.OldTouEntsoe;

public interface OldTariffEntsoeGridSell
		extends OpenemsComponent, TariffGridSell, TariffComponent, Tariff, TariffProvider {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		UNABLE_TO_FETCH_MARKET_PRICES(Doc.of(Level.WARNING)//
				.translationKey(OldTouEntsoe.class, "unableToUpdatePrices")), //
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
