package io.openems.edge.evse.chargepoint.keba.common;

import static io.openems.common.channel.PersistencePriority.HIGH;

import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.evcs.keba.modbus.EvcsKebaModbus;
import io.openems.edge.evcs.keba.udp.EvcsKebaUdp;
import io.openems.edge.evse.chargepoint.keba.common.enums.CableState;
import io.openems.edge.meter.api.ElectricityMeter;

/**
 * Common Channels and methods for {@link EvcsKebaModbus} and
 * {@link EvcsKebaUdp}.
 */
public interface EvcsKeba extends OpenemsComponent, ElectricityMeter {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * PLUG is required for EVCS UI Widget.
		 */
		PLUG(Doc.of(CableState.values())//
				.persistencePriority(HIGH)), //
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
