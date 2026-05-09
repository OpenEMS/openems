package io.openems.edge.batteryinverter.kaco.blueplanetgridsave.errorrestart;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.batteryinverter.kaco.blueplanetgridsave.statemachine.Context;
import io.openems.edge.batteryinverter.kaco.blueplanetgridsave.statemachine.StateMachine;

public interface ErrorRestartBehaviour {

	/**
	 * Handles error state.
	 *
	 * @param context the {@link Context}
	 * @return the next state
	 * @throws OpenemsError.OpenemsNamedException on error
	 */
	StateMachine.State run(Context context) throws OpenemsError.OpenemsNamedException;

}
