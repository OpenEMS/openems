package io.openems.edge.meter.chint.ddsu666;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.SinglePhaseMeter;

public interface MeterChintDdsu666 extends ElectricityMeter, SinglePhaseMeter, OpenemsComponent, ModbusSlave {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * Communication address of the meter.
		 *
		 * <ul>
		 * <li>Interface: MeterChintDdsu666
		 * <li>Type: INTEGER
		 * </ul>
		 */
		COMMUNICATION_ADDRESS(Doc.of(OpenemsType.INTEGER));

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
