package io.openems.edge.meter.janitza.umg104;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.meter.api.ElectricityMeter;

public interface MeterJanitzaUmg104 extends ElectricityMeter, OpenemsComponent, ModbusSlave {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		ROTATION_FIELD(Doc.of(OpenemsType.INTEGER)),

		INTERNAL_TEMPERATURE(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.DEGREE_CELSIUS)), //
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
