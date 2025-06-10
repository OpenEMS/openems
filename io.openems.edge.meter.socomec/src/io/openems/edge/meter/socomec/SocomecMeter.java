package io.openems.edge.meter.socomec;

import io.openems.common.channel.Level;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.meter.api.ElectricityMeter;

public interface SocomecMeter extends ElectricityMeter, OpenemsComponent, ModbusSlave {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		NO_SOCOMEC_METER(Doc.of(Level.FAULT) //
				.text("This is not a Socomec meter")), //
		UNKNOWN_SOCOMEC_METER(Doc.of(Level.FAULT) //
				.text("Unable to identify Socomec meter")), //
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
