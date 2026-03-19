package io.openems.edge.io.shelly.shellypro1pm;

import io.openems.edge.common.channel.Doc;
import io.openems.edge.io.shelly.common.gen2.IoGen2ShellyBase;

public interface IoShellyPro1Pm extends IoGen2ShellyBase {

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
