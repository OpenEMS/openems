package io.openems.edge.ess.mr.gridcon;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;


public interface StateObject {
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
	 * @throws OpenemsNamedException on error
	 */
	void act() throws OpenemsNamedException;
	
	/**
	 * Returns the different state before
	 * @return IState
	 */
	IState getStateBefore();

	/**
	 * Sets the state before, if it is different from itself
	 * @param stateBefore
	 */
	void setStateBefore(IState stateBefore);
	
	void setSubStateObject(StateObject subStateObject);
	StateObject getSubStateObject();
}
