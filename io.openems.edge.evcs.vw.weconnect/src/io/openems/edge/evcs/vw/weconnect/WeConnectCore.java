package io.openems.edge.evcs.vw.weconnect;

import io.openems.edge.common.channel.Doc;

public interface WeConnectCore {

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
