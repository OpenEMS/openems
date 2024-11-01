package io.openems.edge.controller.evcs;

import static io.openems.common.channel.PersistencePriority.HIGH;
import static io.openems.common.types.OpenemsType.BOOLEAN;

import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;

public interface ControllerEvcs extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		AWAITING_HYSTERESIS(Doc.of(BOOLEAN) //
				.persistencePriority(HIGH)) //
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