package io.openems.edge.batteryinverter.cluster;

import org.osgi.service.event.EventHandler;

import io.openems.edge.batteryinverter.api.ManagedSymmetricBatteryInverter;
import io.openems.edge.batteryinverter.api.SymmetricBatteryInverter;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;

public interface BatteryInverterCluster extends SymmetricBatteryInverter, ManagedSymmetricBatteryInverter, 
		OpenemsComponent, ModbusSlave { // TODO: BatteryInverterMeta

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
