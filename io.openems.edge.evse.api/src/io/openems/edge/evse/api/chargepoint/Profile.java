package io.openems.edge.evse.api.chargepoint;

import io.openems.edge.evse.api.Limit;
import io.openems.edge.evse.api.chargepoint.Profile.PhaseSwitchToSinglePhase;
import io.openems.edge.evse.api.chargepoint.Profile.PhaseSwitchToThreePhase;

public sealed interface Profile permits PhaseSwitchToSinglePhase, PhaseSwitchToThreePhase {

	public sealed interface Command permits PhaseSwitchToSinglePhase.Command, PhaseSwitchToThreePhase.Command {
	}

	/**
	 * Profile for Phase-Switching from Three-Phase to Single-Phase.
	 */
	public static record PhaseSwitchToSinglePhase(Limit singlePhaseLimit) implements Profile {
		/**
		 * Builds a {@link PhaseSwitchToSinglePhase.Command}.
		 * 
		 * @return a {@link PhaseSwitchToSinglePhase.Command}
		 */
		public Command command() {
			return new Command();
		}

		public static record Command() implements Profile.Command {
		}
	}

	/**
	 * Profile for Phase-Switching from Single-Phase to Three-Phase.
	 */
	public static record PhaseSwitchToThreePhase(Limit threePhaseLimit) implements Profile {
		/**
		 * Builds a {@link PhaseSwitchToThreePhase.Command}.
		 * 
		 * @return a {@link PhaseSwitchToThreePhase.Command}
		 */
		public Command command() {
			return new Command();
		}

		public static record Command() implements Profile.Command {
		}
	}
}