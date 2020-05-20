package io.openems.edge.kaco.blueplanet.hybrid10.ess;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.kaco.blueplanet.hybrid10.BatteryStatus;
import io.openems.edge.kaco.blueplanet.hybrid10.InverterStatus;

public interface BpEss {

	public static enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		BMS_VOLTAGE(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.VOLT)), //
		RISO(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.OHM)), //
		SURPLUS_FEED_IN(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		INVERTER_STATUS(Doc.of(InverterStatus.values())), //
		BATTERY_STATUS(Doc.of(BatteryStatus.values())); //

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