package io.openems.edge.edge2edge.websocket.dccharger;

import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.ess.dccharger.api.EssDcCharger;

public interface Edge2EdgeWebsocketDcCharger extends OpenemsComponent, EssDcCharger {

	enum ChannelId implements io.openems.edge.common.channel.ChannelId {
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
