package io.openems.edge.ess.adstec.storaxe;

import io.openems.edge.common.channel.Doc;

/**
 * This file should contain all the extra channels apart from the standard
 * natures that the StoraXe provides.
 *
 * @author les
 *
 */
public interface EssAdstecStoraxe {

	public static enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		// At the moment we don't implement any custom channels.
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