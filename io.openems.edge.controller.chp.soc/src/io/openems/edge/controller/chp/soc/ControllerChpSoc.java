package io.openems.edge.controller.chp.soc;

import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;

public interface ControllerChpSoc extends Controller, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		MODE(Doc.of(Mode.values()) //
				.initialValue(Mode.AUTOMATIC) //
				.text("Configured Mode")), //
		STATE_MACHINE(Doc.of(State.values()) //
				.text("Current State of State-Machine"));

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
