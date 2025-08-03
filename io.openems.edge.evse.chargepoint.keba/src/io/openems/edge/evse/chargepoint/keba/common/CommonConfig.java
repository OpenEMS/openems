package io.openems.edge.evse.chargepoint.keba.common;

import io.openems.edge.common.type.Phase.SingleOrThreePhase;
import io.openems.edge.evse.api.chargepoint.PhaseRotation;
import io.openems.edge.evse.chargepoint.keba.common.enums.LogVerbosity;

public record CommonConfig(boolean readOnly, PhaseRotation phaseRotation, boolean p30hasS10PhaseSwitching,
		SingleOrThreePhase wiring, LogVerbosity logVerbosity) {

	/**
	 * Builds {@link CommonConfig} from Modbus Config.
	 * 
	 * @param config the Modbus Config
	 * @return the record
	 */
	public static CommonConfig from(io.openems.edge.evse.chargepoint.keba.modbus.Config config) {
		return new CommonConfig(config.readOnly(), config.phaseRotation(), config.p30hasS10PhaseSwitching(),
				config.wiring(), config.logVerbosity());
	}

	/**
	 * Builds {@link CommonConfig} from UDP Config.
	 * 
	 * @param config the UDP Config
	 * @return the record
	 */
	public static CommonConfig from(io.openems.edge.evse.chargepoint.keba.udp.Config config) {
		return new CommonConfig(config.readOnly(), config.phaseRotation(), config.p30hasS10PhaseSwitching(),
				config.wiring(), config.logVerbosity());
	}
}
