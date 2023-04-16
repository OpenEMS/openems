package io.openems.edge.meter.socomec.singlephase;

import io.openems.common.channel.Level;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.SinglePhaseMeter;
import io.openems.edge.meter.socomec.SocomecMeter;

public interface SocomecMeterSinglephase
		extends SocomecMeter, SinglePhaseMeter, ElectricityMeter, OpenemsComponent, ModbusSlave {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		NOT_A_SINGLEPHASE_METER(Doc.of(Level.FAULT) //
				.text("This is not a singlephase meter")), //
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
