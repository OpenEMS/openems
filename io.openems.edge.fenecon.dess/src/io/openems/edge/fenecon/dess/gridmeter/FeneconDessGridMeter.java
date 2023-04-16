package io.openems.edge.fenecon.dess.gridmeter;

import org.osgi.service.event.EventHandler;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.timedata.api.TimedataProvider;

public interface FeneconDessGridMeter extends ElectricityMeter, OpenemsComponent, TimedataProvider, EventHandler {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		ORIGINAL_ACTIVE_PRODUCTION_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)),
		ORIGINAL_ACTIVE_CONSUMPTION_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS));

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