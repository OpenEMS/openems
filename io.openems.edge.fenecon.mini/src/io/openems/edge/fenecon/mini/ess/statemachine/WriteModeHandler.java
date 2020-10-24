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
		// Set correct Debug Run State
		DebugRunState runState = context.component.getDebugRunState();
		if (context.setActivePower > 0 && runState != DebugRunState.DISCHARGE) {
			context.component.setDebugRunState(DebugRunState.DISCHARGE);
		} else if (context.setActivePower < 0 && runState != DebugRunState.CHARGE) {
			context.component.setDebugRunState(DebugRunState.CHARGE);
		}

		// Adjust Active Power
		int power = WriteModeHandler.adjustActivePower(context.setActivePower);

		int current = Math.round((power / 230F) * 1000); // [mA]

		if (context.setActivePower >= 0) {
			// Set Discharge & no Charge
			if (context.component.getGridMaxChargeCurrent().orElse(-1) != 0) {
				context.component.setGridMaxChargeCurrent(0);
			}
			if (context.component.getGridMaxDischargeCurrent().orElse(-1) != current) {
				context.component.setGridMaxDischargeCurrent(current);
			}
		} else {
			// Set Charge & no Discharge
			if (context.component.getGridMaxDischargeCurrent().orElse(-1) != 0) {
				context.component.setGridMaxDischargeCurrent(0);
			}
			if (context.component.getGridMaxChargeCurrent().orElse(-1) != current) {
				context.component.setGridMaxChargeCurrent(current);
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
		} else if (p > 0) {
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
			p *= 1;
			if (p < 75) {
				return 200;
			} else {
				float m = 75 / 2800;
				float t = 125;
				return Math.round(p + m * p + t);
			}
		}
	}

}
