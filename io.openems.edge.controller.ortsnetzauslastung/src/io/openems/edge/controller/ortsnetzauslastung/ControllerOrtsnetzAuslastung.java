package io.openems.edge.controller.ortsnetzauslastung;

import io.openems.common.channel.Level;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;

public interface ControllerOrtsnetzAuslastung extends Controller, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		UNABLE_TO_SEND(Doc.of(Level.WARNING)//
				.translationKey(ControllerOrtsnetzAuslastung.class, "unableToSend")), //

		NO_COORDINATES(Doc.of(Level.WARNING)//
				.translationKey(ControllerOrtsnetzAuslastung.class, "noCoordinates")), //
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
