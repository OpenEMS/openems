package io.openems.edge.evcs.ocpp.ies.keywatt.singleccs;

import org.osgi.service.event.EventHandler;

import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.MeasuringEvcs;
import io.openems.edge.evcs.api.SocEvcs;
import io.openems.edge.meter.api.ElectricityMeter;

public interface EvcsOcppIesKeywattSingle
		extends Evcs, ManagedEvcs, MeasuringEvcs, ElectricityMeter, OpenemsComponent, EventHandler, SocEvcs {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
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
