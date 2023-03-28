package io.openems.edge.solaredge.charger;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerDoc;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.ess.dccharger.api.EssDcCharger;
import io.openems.edge.solaredge.hybrid.ess.SolarEdgeHybridEss;


public interface SolaredgeDcCharger extends EssDcCharger, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		DEBUG_SET_PV_POWER_LIMIT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)),

		/**
		 * Curtail PV. Careful: this channel is shared between both Chargers.
		 */
		SET_PV_POWER_LIMIT(new IntegerDoc() //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.WRITE_ONLY) //
				.onInit(new IntegerWriteChannel.MirrorToDebugChannel(ChannelId.DEBUG_SET_PV_POWER_LIMIT))), //

		// LongReadChannel
		BMS_DCDC_OUTPUT_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)); //

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
