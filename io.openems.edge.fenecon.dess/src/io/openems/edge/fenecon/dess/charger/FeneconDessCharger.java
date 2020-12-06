package io.openems.edge.fenecon.dess.charger;

import io.openems.edge.common.channel.Doc;
import io.openems.edge.ess.dccharger.api.EssDcCharger;

public interface FeneconDessCharger extends EssDcCharger {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
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
