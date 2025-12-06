package io.openems.edge.evcs.openwb;

import io.openems.common.channel.Level;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.evcs.api.Evcs;

public interface EvcsOpenWb extends Evcs, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * Slave Communication Failed Fault.
		 *
		 * <p>
		 * Indicates a failure in communication with the OpenWB charger, which might
		 * affect system operations.
		 *
		 * <ul>
		 * <li>Interface: EvcsOpenWb
		 * <li>Type: State
		 * </ul>
		 */
		SLAVE_COMMUNICATION_FAILED(Doc.of(Level.FAULT)//
				.text("Communication with OpenWB charger failed."));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#SLAVE_COMMUNICATION_FAILED} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setSlaveCommunicationFailed(boolean value) {
		this.channel(ChannelId.SLAVE_COMMUNICATION_FAILED).setNextValue(value);
	}
}
