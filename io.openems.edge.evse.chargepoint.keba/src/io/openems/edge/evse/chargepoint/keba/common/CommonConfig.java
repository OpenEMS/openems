package io.openems.edge.evse.chargepoint.keba.common;

import io.openems.edge.evse.api.SingleThreePhase;
import io.openems.edge.evse.api.chargepoint.PhaseRotation;
import io.openems.edge.evse.chargepoint.keba.common.enums.P30S10PhaseSwitching;

public record CommonConfig(boolean readOnly, PhaseRotation phaseRotation, P30S10PhaseSwitching p30s10PhaseSwitching,
		SingleThreePhase wiring, boolean debugMode) {

	/**
	 * Builds {@link CommonConfig} from Modbus Config.
	 * 
	 * @param config the Modbus Config
	 * @return the record
	 */
	public static CommonConfig from(io.openems.edge.evse.chargepoint.keba.modbus.Config config) {
		return new CommonConfig(config.readOnly(), config.phaseRotation(), config.p30S10PhaseSwitching(),
				config.wiring(), config.debugMode());
	}

	/**
	 * Builds {@link CommonConfig} from UDP Config.
	 * 
	 * @param config the UDP Config
	 * @return the record
	 */
	public static CommonConfig from(io.openems.edge.evse.chargepoint.keba.udp.Config config) {
		return new CommonConfig(config.readOnly(), config.phaseRotation(), config.p30S10PhaseSwitching(),
				config.wiring(), config.debugMode());
	}
}
