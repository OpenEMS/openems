package io.openems.edge.ess.generic.symmetric;

import io.openems.edge.common.channel.Doc;
import io.openems.edge.ess.api.ManagedSymmetricEss;

public interface GenericManagedSymmetricEss extends ManagedSymmetricEss {

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
