package io.openems.edge.evse.chargepoint.keba.common;

import static io.openems.common.channel.PersistencePriority.HIGH;
import static io.openems.common.types.OpenemsType.INTEGER;

import io.openems.common.channel.Unit;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.evcs.keba.modbus.EvcsKebaModbusImpl;
import io.openems.edge.evcs.keba.udp.EvcsKebaUdpImpl;
import io.openems.edge.evse.chargepoint.keba.common.enums.CableState;
import io.openems.edge.meter.api.ElectricityMeter;

/**
 * Common Channels and methods for {@link EvcsKebaModbusImpl} and
 * {@link EvcsKebaUdpImpl}.
 */
public interface EvcsKeba extends OpenemsComponent, ElectricityMeter {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * PLUG is required for EVCS UI Widget.
		 */
		PLUG(Doc.of(CableState.values())//
				.persistencePriority(HIGH)), //

		MAX_HARDWARE_CURRENT(Doc.of(INTEGER)//
				.unit(Unit.MILLIAMPERE)), //
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
