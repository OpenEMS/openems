package io.openems.edge.airconditioner.hydac4kw;

import org.osgi.service.event.EventHandler;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;

public interface HydacAirConditioner extends OpenemsComponent, EventHandler {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		ERROR_1(Doc.of(ErrorSignal.values()).accessMode(AccessMode.READ_ONLY)),
		ERROR_2(Doc.of(ErrorSignal.values()).accessMode(AccessMode.READ_ONLY)),
		ON(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE));
//		START_COOLDOWN(Doc.of(Level.WARNING));
		
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
