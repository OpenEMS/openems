package io.openems.edge.io.shelly.shellypluspm;

import io.openems.common.channel.Level;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.io.shelly.shellyplugsbase.IoShellyPlugSBase;

public interface IoShellyPlusPm extends IoShellyPlugSBase {

	public static enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * Indicates whether the Shelly needs a restart.
		 *
		 * <ul>
		 * <li>Interface: ShellyPlusPM
		 * <li>Type: Boolean
		 * <li>Level: INFO
		 * </ul>
		 */
		NEEDS_RESTART(Doc.of(Level.INFO) //
				.text("Shelly suggests a restart.")); //

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
