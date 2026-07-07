package io.openems.edge.braiinsos;

import org.osgi.service.event.EventHandler;

import io.openems.common.channel.Level;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;

public interface ControllerBraiinsSingle extends OpenemsComponent, EventHandler {

	enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * Communication Failed Fault.
		 *
		 * <ul>
		 * <li>Interface: Braiins
		 * <li>Type: State
		 * </ul>
		 */
		COMMUNICATION_FAILED(Doc.of(Level.WARNING)), //

		EFFICIENCY(Doc.of(OpenemsType.DOUBLE)), //

		REAL_HASHRATE_LAST_15S(Doc.of(OpenemsType.DOUBLE)), //

		EFFECTIVE_MODE(Doc.of(Mode.values())//
				.text("Mode effectively applied by the device")//
				.persistencePriority(PersistencePriority.HIGH)), //

		;

		private final Doc doc;

		ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

}
