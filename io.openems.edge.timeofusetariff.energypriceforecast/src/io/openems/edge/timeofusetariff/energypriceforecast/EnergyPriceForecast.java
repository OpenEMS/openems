package io.openems.edge.timeofusetariff.energypriceforecast;

import io.openems.common.channel.Level;
import io.openems.common.tariff.api.spottariff.SpotTariffProvider;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.timeofusetariff.api.TariffComponent;

public interface EnergyPriceForecast extends OpenemsComponent, TariffComponent, SpotTariffProvider {

	enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		UPDATE_FAILURE(Doc.of(Level.WARNING).text("Unable to update the electricity price forecast"));

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
