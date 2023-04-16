package io.openems.edge.meter.socomec.threephase;

import io.openems.common.channel.Level;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.socomec.SocomecMeter;

public interface SocomecMeterThreephase extends SocomecMeter, ElectricityMeter, OpenemsComponent, ModbusSlave {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		NOT_A_THREEPHASE_METER(Doc.of(Level.FAULT) //
				.text("This is not a threephase meter")), //
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
