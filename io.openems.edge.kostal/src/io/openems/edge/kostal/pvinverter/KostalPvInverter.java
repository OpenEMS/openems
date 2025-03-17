package io.openems.edge.kostal.pvinverter;

import org.osgi.service.event.EventHandler;

import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.ess.dccharger.api.EssDcCharger;

public interface KostalPvInverter
		extends
			//ManagedSymmetricPvInverter,
			//ElectricityMeter,
			EssDcCharger,
			OpenemsComponent,
			EventHandler,
			ModbusSlave {

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
