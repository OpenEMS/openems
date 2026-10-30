package io.openems.edge.timeofusetariff.api;

import io.openems.common.channel.Level;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;

public interface TariffComponent {

	enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		LAST_UPDATE(Doc.of(OpenemsType.LONG)//
				.unit(Unit.SECONDS)),

		NO_TARIFFS_REQUESTED(Doc.of(Level.INFO)//
				.translationKey(TariffComponent.class, "noTariffsRequested")), //

		NO_PRICES(Doc.of(Level.FAULT)//
				.translationKey(TariffComponent.class, "noPrices")), //

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
