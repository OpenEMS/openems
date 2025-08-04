package io.openems.edge.evse.chargepoint.keba.common;

import static io.openems.common.channel.AccessMode.WRITE_ONLY;
import static io.openems.common.types.OpenemsType.INTEGER;

import io.openems.common.channel.Unit;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
import io.openems.edge.evse.chargepoint.keba.common.enums.PhaseSwitchSource;
import io.openems.edge.evse.chargepoint.keba.modbus.EvseKebaModbusImpl;
import io.openems.edge.evse.chargepoint.keba.udp.EvseKebaUdpImpl;

/**
 * Common Channels and methods for {@link EvseKebaModbusImpl} and
 * {@link EvseKebaUdpImpl}.
 */
public interface EvseKeba extends Keba, EvseChargePoint {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		ENERGY_SESSION(Doc.of(INTEGER)), // Just FYI. Manually calculated in Evse.Single.Controller
		SET_ENERGY_LIMIT(Doc.of(INTEGER)//
				.unit(Unit.WATT_HOURS)//
				.accessMode(WRITE_ONLY)), //
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

	/**
	 * Gets the required {@link PhaseSwitchSource} for this implementation.
	 * 
	 * @return the {@link PhaseSwitchSource}
	 */
	public PhaseSwitchSource getRequiredPhaseSwitchSource();
}
