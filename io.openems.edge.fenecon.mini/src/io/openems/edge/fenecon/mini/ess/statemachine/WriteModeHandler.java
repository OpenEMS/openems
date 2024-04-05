package io.openems.edge.fenecon.mini.ess.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.fenecon.mini.ess.DebugRunState;
import io.openems.edge.fenecon.mini.ess.statemachine.StateMachine.State;

public class WriteModeHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		// Apply Active and Reactive Power Set-Points
		this.applyPower(context);

		return State.WRITE_MODE;
	}

	/**
	 * Applies the Active and Reactive Power Set-Points.
	 *
	 * @param context the {@link Context}
	 * @throws OpenemsNamedException on error
	 */
	private void applyPower(Context context) throws OpenemsNamedException {
		var ess = context.getParent();

		// Set correct Debug Run State
		var runState = ess.getDebugRunState();
		if (context.setActivePower > 0 && runState != DebugRunState.DISCHARGE) {
			ess.setDebugRunState(DebugRunState.DISCHARGE);
		} else if (context.setActivePower < 0 && runState != DebugRunState.CHARGE) {
			ess.setDebugRunState(DebugRunState.CHARGE);
		}

		// Adjust Active Power
		var power = WriteModeHandler.adjustActivePower(context.setActivePower);

		var current = Math.round(power / 230F * 1000); // [mA]

		if (context.setActivePower >= 0) {
			// Set Discharge & no Charge
			if (ess.getGridMaxChargeCurrent().orElse(-1) != 0) {
				ess.setGridMaxChargeCurrent(0);
			}
			if (ess.getGridMaxDischargeCurrent().orElse(-1) != current) {
				ess.setGridMaxDischargeCurrent(current);
			}
		} else {
			// Set Charge & no Discharge
			if (ess.getGridMaxDischargeCurrent().orElse(-1) != 0) {
				ess.setGridMaxDischargeCurrent(0);
			}
			if (ess.getGridMaxChargeCurrent().orElse(-1) != current) {
				ess.setGridMaxChargeCurrent(current);
			}
		}
	}

	/**
	 * Converts the ActivePower Set-Point to a value found by experimenting with the
	 * system.
	 *
	 * @param p the Set-Point (negative for charge; positive for discharge)
	 * @return the adjusted Set-Point (always positive)
	 */
	private static int adjustActivePower(int p) {
		if (p == 0) {
			return 0;
		}
		if (p > 0) {
			// discharge
			if (p < 70) {
				return 90;
			} else if (p < 100) {
				return p + 20;
			} else if (p < 200) {
				return p + 30;
			} else if (p < 300) {
				return p + 40;
			} else {
				return p + 55;
			}
		} else {
			// charge
			p *= -1;
			if (p < 75) {
				return 200;
			} else {
				float m = 75 / 2800;
				var t = 125F;
				return Math.round(p + m * p + t);
			}
		}
	}

}
