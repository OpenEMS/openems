package io.openems.edge.controller.evse.single;

import io.openems.edge.common.type.Phase;

public enum PhaseSwitching {
	/**
	 * Phase-Switching is disabled.
	 */
	DISABLE, //
	/**
	 * Phase-Switching forced to SINGLE_PHASE.
	 */
	FORCE_SINGLE_PHASE, //
	/**
	 * Phase-Switching force to THREE_PHASE.
	 */
	FORCE_THREE_PHASE, //
	/**
	 * Phase-Switching automatically adapts based on mode:.
	 * 
	 * <ul>
	 * <li>PV (SURPLUS): Switches between single and three phase depending on
	 * available power.
	 * <li>PV+Min (MINIMUM): Single phase if no PV surplus, otherwise like PV.
	 * <li>Force Charge (FORCE): Three phase with max power.
	 * </ul>
	 */
	AUTOMATIC, //

	;

	public static final int AUTOMATIC_SINGLE_TO_THREE_PHASE_SWITCH_POWER = 4100;
	public static final int AUTOMATIC_THREE_TO_SINGLE_PHASE_SWITCH_POWER = 3700;

	public Phase.SingleOrThreePhase getForcePhase() {
		return switch (this) {
		case DISABLE -> null;
		case FORCE_SINGLE_PHASE -> Phase.SingleOrThreePhase.SINGLE_PHASE;
		case FORCE_THREE_PHASE -> Phase.SingleOrThreePhase.THREE_PHASE;
		case AUTOMATIC -> null;
		};
	}
}