package io.openems.edge.io.shelly.shellypluspmmini;

import org.osgi.service.event.EventHandler;

import io.openems.common.channel.Level;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.SinglePhaseMeter;

public interface IoShellyPlusPmMini extends SinglePhaseMeter, ElectricityMeter, OpenemsComponent, EventHandler {

	public static enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * Indicates whether the Shelly needs a restart.
		 *
		 * <ul>
		 * <li>Interface: ShellyPlusPMMini
		 * <li>Type: State
		 * <li>Level: INFO
		 * </ul>
		 */
		NEEDS_RESTART(Doc.of(Level.INFO)//
				.text("Shelly suggests a restart.")),
		/**
		 * Slave Communication Failed Fault.
		 *
		 * <ul>
		 * <li>Interface: ShellyPlusPMMini
		 * <li>Type: State
		 * <li>Level: FAULT
		 * </ul>
		 */
		SLAVE_COMMUNICATION_FAILED(Doc.of(Level.FAULT)); //

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
