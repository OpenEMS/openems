package io.openems.edge.evcs.dezony;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;

public interface Dezony {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		RAW_CHARGE_STATUS_CHARGEPOINT(Doc.of(OpenemsType.STRING));

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