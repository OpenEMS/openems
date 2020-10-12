package io.openems.edge.kaco.blueplanet.hybrid10.vectis;

import io.openems.edge.common.channel.Doc;

public interface Vectis {

	public static enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		VECTIS_STATUS(Doc.of(VectisStatus.values())) //
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