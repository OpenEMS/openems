package io.openems.edge.edge2edge.websocket.genericreadcomponent;

import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;

public interface Edge2EdgeGenericReadComponent extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		; //

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
