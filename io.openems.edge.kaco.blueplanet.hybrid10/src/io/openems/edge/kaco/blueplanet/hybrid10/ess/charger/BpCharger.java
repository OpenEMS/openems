package io.openems.edge.kaco.blueplanet.hybrid10.ess.charger;

import io.openems.edge.common.channel.Doc;

public interface BpCharger {

	public static enum ChannelId implements io.openems.edge.common.channel.ChannelId {
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
