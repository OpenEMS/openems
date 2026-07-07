package io.openems.edge.evse.chargepoint.keba.common;

import io.openems.edge.common.type.Phase.SingleOrThreePhase;
import io.openems.edge.evse.chargepoint.keba.common.enums.LogVerbosity;
import io.openems.edge.meter.api.PhaseRotation;

public record CommonConfig(boolean readOnly, PhaseRotation phaseRotation, boolean supportsPhaseSwitching,
		SingleOrThreePhase wiring, LogVerbosity logVerbosity) {

	/**
	 * Builds {@link CommonConfig} from Modbus Config for a KEBA P40.
	 * 
	 * @param config the Modbus Config
	 * @return the record
	 */
	public static CommonConfig from(io.openems.edge.evse.chargepoint.keba.modbus.Config config) {
		return new CommonConfig(config.readOnly(), config.phaseRotation(), true /* supportsPhaseSwitching */,
				config.wiring(), config.logVerbosity());
	}

	/**
	 * Builds {@link CommonConfig} from UDP Config for a KEBA P30.
	 * 
	 * @param config the UDP Config
	 * @return the record
	 */
	public static CommonConfig from(io.openems.edge.evse.chargepoint.keba.udp.Config config) {
		return new CommonConfig(config.readOnly(), config.phaseRotation(), config.p30hasS10PhaseSwitching(),
				config.wiring(), config.logVerbosity());
	}
}
