package io.openems.edge.goodwe.emergencypowermeter;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.meter.api.ElectricityMeter;

public interface GoodWeEmergencyPowerMeter extends ElectricityMeter, OpenemsComponent {

	public static enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		FREQUENCY_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.HERTZ)), //
		FREQUENCY_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.HERTZ)), //
		FREQUENCY_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.HERTZ));

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
