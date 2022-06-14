package io.openems.edge.pvinverter.sunspec;

import io.openems.common.channel.Level;
import io.openems.edge.common.channel.Doc;

public interface SunSpecPvInverter {

	public static enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		PV_LIMIT_FAILED(Doc.of(Level.FAULT) //
				.text("PV-Limit failed")), //
		WRONG_PHASE_CONFIGURED(Doc.of(Level.WARNING) //
				.text("Configured Phase does not match the Model")), //
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