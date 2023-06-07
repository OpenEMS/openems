package io.openems.edge.meter.janitza.umg96rme;

import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.SymmetricMeter;

/**
 * Implements the Janitza UMG 96RM-E power analyzer.
 *
 * <p>
 * https://www.janitza.com/umg-96rm-e.html
 */
public interface MeterJanitzaUmg96rme
		extends SymmetricMeter, AsymmetricMeter, ModbusComponent, OpenemsComponent, ModbusSlave {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
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
