package io.openems.edge.controller.ess.fastfrequencyreserve.statemachine;

import java.time.ZonedDateTime;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.ess.fastfrequencyreserve.statemachine.StateMachine.State;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;

public class PreActivationHandler extends StateHandler<State, Context> {

	private static final double EIGHTEENX_PERCENT_OF_MAX_POWER = 0.18; // [%]

	@Override
	protected State runAndGetNextState(Context context) throws OpenemsNamedException {
		var ess = context.ess;
		int minPowerEss = this.calculateMinPower(ess);

		if (this.isActivationTime(context)) {
			return State.ACTIVATION_TIME;
		} else {
			ess.setActivePowerEquals(minPowerEss);
			return State.PRE_ACTIVATION_STATE;
		}
	}

	/**
	 * Calculates 18% of the minimum power of the given ess.
	 *
	 * @param ess The managed symmetric ess.
	 * @return 18% of the minimum power of the ess.
	 */
	private int calculateMinPower(ManagedSymmetricEss ess) {
		return (int) (ess.getPower().getMinPower(ess, Phase.ALL, Pwr.ACTIVE) * EIGHTEENX_PERCENT_OF_MAX_POWER);
	}

	/**
	 * Checks if the current time, as adjusted by the component manager's clock, is
	 * within the activation time window.
	 *
	 * 
	 * @param context the context
	 * @return {@code true} if the current time is within the activation time
	 *         window, {@code false} otherwise.
	 */
	private boolean isActivationTime(Context context) {
		var currentEpochSecond = ZonedDateTime.now(context.clock).toEpochSecond();
		return currentEpochSecond >= context.startTimestamp
				&& currentEpochSecond <= context.startTimestamp + context.duration;
	}
}