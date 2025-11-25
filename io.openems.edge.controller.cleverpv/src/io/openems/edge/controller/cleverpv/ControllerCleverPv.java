package io.openems.edge.controller.cleverpv;

import static io.openems.common.channel.PersistencePriority.HIGH;
import static io.openems.common.channel.Unit.CUMULATED_SECONDS;
import static io.openems.common.types.OpenemsType.LONG;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.PersistencePriority;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;

public interface ControllerCleverPv extends Controller, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		UNABLE_TO_SEND(Doc.of(Level.WARNING) //
				.translationKey(ControllerCleverPv.class, "unableToSend")), //

		REMOTE_CONTROL_MODE(Doc.of(RemoteControlMode.values())//
				.persistencePriority(PersistencePriority.HIGH)//
				.accessMode(AccessMode.READ_ONLY)),

		CUMULATED_NO_DISCHARGE_TIME(Doc.of(LONG)//
				.unit(CUMULATED_SECONDS)//
				.persistencePriority(HIGH)), //

		CUMULATED_INACTIVE_TIME(Doc.of(LONG)//
				.unit(CUMULATED_SECONDS)//
				.persistencePriority(HIGH)), //
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