package io.openems.edge.timeofusetariff.spottariff.oldentsoe.gridbuy;

import io.openems.common.channel.Level;
import io.openems.common.tariff.api.Tariff;
import io.openems.common.tariff.api.TariffProvider;
import io.openems.common.tariff.api.spottariff.SpotTariffProvider;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.timeofusetariff.api.TariffComponent;
import io.openems.edge.timeofusetariff.api.TimeOfUseTariff;

public interface OldTouEntsoe
		extends OpenemsComponent, TimeOfUseTariff, TariffComponent, TariffProvider, Tariff, SpotTariffProvider {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		UNABLE_TO_UPDATE_PRICES(Doc.of(Level.WARNING)//
				.translationKey(OldTouEntsoe.class, "unableToUpdatePrices")), //
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
