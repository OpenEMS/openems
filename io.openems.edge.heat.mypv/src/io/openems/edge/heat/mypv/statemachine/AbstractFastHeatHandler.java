package io.openems.edge.heat.mypv.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.heat.mypv.statemachine.StateMachine.State;

/**
 * Abstract base class for fast-heat mode handlers, providing common heating
 * power application logic.
 */
public abstract class AbstractFastHeatHandler extends StateHandler<State, Context> {

	/**
	 * Applies the maximum heating power to the heat element.
	 *
	 * @param context the state machine context
	 * @throws OpenemsNamedException on error
	 */
	protected void applyMaxHeatPower(Context context) throws OpenemsNamedException {
		final var maxPower = context.getMaxHeatPower();
		context.setTargetActivePowerForHeatElement(maxPower);
	}
}
