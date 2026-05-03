package io.openems.edge.heat.askoma.statemachine;

import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.heat.askoma.statemachine.StateMachine.State;

/**
 * Abstract base class for fast-heat mode handlers, providing common heating
 * power application logic.
 */
public abstract class AbstractFastHeatHandler extends StateHandler<State, Context> {

	private static final int OFFSET_50_W = 50;

	/**
	 * Applies the maximum heating power (configured max + 50 W offset) to the heat
	 * element.
	 *
	 * <p>
	 * The +50 W offset ensures the heater reaches the configured maximum power
	 * step.
	 *
	 * @param context the state machine context
	 */
	protected void applyMaxHeatPower(Context context) {
		final var maxPower = context.getMaxHeatPower() + OFFSET_50_W;
		context.setTargetActivePowerForHeatElement(-maxPower);
	}
}
