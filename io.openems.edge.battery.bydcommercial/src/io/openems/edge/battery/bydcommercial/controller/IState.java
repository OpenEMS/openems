package io.openems.edge.battery.bydcommercial.controller;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;

public interface IState {

	/**
	 * Returns the corresponding state.
	 * 
	 * @return the state
	 */
	State getState();

	/**
	 * Depending on the circumstances the next state according to the state machine
	 * is returned.
	 * 
	 * @return the next state
	 */
	State getNextState();

	/**
	 * in this method everything should be executed what there is to do in this
	 * state.
	 * 
	 * @throws OpenemsNamedException on error
	 */
	void act() throws OpenemsNamedException;

}
