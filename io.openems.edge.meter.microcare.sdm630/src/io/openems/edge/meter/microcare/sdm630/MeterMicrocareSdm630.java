package io.openems.edge.meter.microcare.sdm630;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;

public interface MeterMicrocareSdm630 {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		REACTIVE_PRODUCTION_ENERGY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS)), //
		REACTIVE_CONSUMPTION_ENERGY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS)), //
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
