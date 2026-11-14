package io.openems.edge.timeofusetariff.spottariff.provider.backend;

import io.openems.common.channel.Level;
import io.openems.common.tariff.api.spottariff.SpotTariffProvider;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.timeofusetariff.api.TariffComponent;
import io.openems.edge.timeofusetariff.spottariff.provider.AbstractMirroredSpotTariffProviderComponent;

public interface BackendTariffProvider extends OpenemsComponent, TariffComponent, SpotTariffProvider {

	enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		NO_BACKEND_CONNECTION(Doc.of(Level.WARNING)//
				.translationKey(AbstractMirroredSpotTariffProviderComponent.class, "backend.noConnection")),

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
