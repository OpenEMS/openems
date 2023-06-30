package io.openems.edge.ess.mr.gridcon.state.gridconstate;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.ess.mr.gridcon.GridconSettings;
import io.openems.edge.ess.mr.gridcon.IState;

public interface GridconStateObject {
	/**
	 * Returns the corresponding state.
	 * 
	 * @return the state
	 */
	IState getState();

	/**
	 * Depending on the circumstances the next state according to the state machine
	 * is returned.
	 * 
	 * @return the next state
	 */
	IState getNextState();

	/**
	 * in this method everything should be executed what there is to do in this
	 * state.
	 * 
	 * @param gridconSettings the {@link GridconSettings}
	 * @throws OpenemsNamedException on error
	 */
	void act(GridconSettings gridconSettings) throws OpenemsNamedException;

}
