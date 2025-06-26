package io.openems.edge.fenecon.dess.charger;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.ess.dccharger.api.EssDcCharger;

public interface FeneconDessCharger extends EssDcCharger {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		ORIGINAL_ACTUAL_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.CUMULATED_WATT_HOURS));

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
