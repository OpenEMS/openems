package io.openems.edge.controller.evse.single;

import io.openems.edge.controller.evse.single.Types.Hysteresis;
import io.openems.edge.evse.api.chargepoint.Mode;

/**
 * Parameters of one Evse.Controller.Single. Contains configuration settings,
 * runtime parameters and CombinedAbilities of Charge-Point and
 * Electric-Vehicle.
 */
public record Params(//
		/**
		 * Mode configuration of Evse.Controller.Single.
		 */
		Mode.Actual actualMode, //
		/**
		 * The measured ActivePower; possibly null.
		 */
		Integer activePower, //
		/**
		 * Hysteresis data
		 */
		Hysteresis hysteresis, //
		/**
		 * PhaseSwitching configuration of Evse.Controller.Single.
		 */
		PhaseSwitching phaseSwitching, //
		/**
		 * EV appears to be fully charged.
		 */
		boolean appearsToBeFullyCharged, //
		/**
		 * The CombinedAbilities of Charge-Point and Electric-Vehicle.
		 */
		CombinedAbilities combinedAbilities) {
}
