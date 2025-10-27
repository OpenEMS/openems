package io.openems.edge.controller.evse.single;

import io.openems.edge.controller.evse.single.Types.History;
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
		 * History data
		 */
		History history, //
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

	public Params(Mode.Actual actualMode, Integer activePower, History history, PhaseSwitching phaseSwitching,
			CombinedAbilities combinedAbilities) {
		this(actualMode, activePower, history, Hysteresis.from(history), phaseSwitching,
				history.getAppearsToBeFullyCharged(), combinedAbilities);
	}

}
