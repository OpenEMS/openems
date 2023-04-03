package io.openems.edge.airconditioner.hydac4kw;

import org.osgi.service.event.EventHandler;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.common.startstoppratelimited.RateLimitedStartStoppable;

public interface HydacAirConditioner extends OpenemsComponent, EventHandler, RateLimitedStartStoppable, StartStoppable {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		ERROR_1(Doc.of(ErrorSignal.values()).accessMode(AccessMode.READ_ONLY)),
		ERROR_2(Doc.of(ErrorSignal.values()).accessMode(AccessMode.READ_ONLY)),
		START_EXCEEDED(Doc.of(Level.WARNING));
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
