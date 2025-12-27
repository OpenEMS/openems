package io.openems.edge.victron.meter.grid;

import static io.openems.common.types.OpenemsType.STRING;

import io.openems.edge.common.channel.Doc;
import io.openems.edge.meter.api.ElectricityMeter;

public interface VictronMeter extends ElectricityMeter {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		SERIAL_NUMBER(Doc.of(STRING)//
		);

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
