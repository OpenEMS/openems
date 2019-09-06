package io.openems.edge.controller.ess.limitdischargecellvoltage;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;

//TODO Maybe it is more appropriate if IState extends OptionsEnum to make it easier to write it in channels
//Or is it necessary to be such a value, maybe a String is enough? Or a new Interface for Statemachine objects?
// It may be important for a state to know what was the state before, also it is possibly important how often the state
//was reached or s.th. else, so the state object should be singletons?!

public interface IState {

	/**
	 * Returns the corresponding state 
	 * @return
	 */
	State getState();

	/**
	 * Depending on the circumstances the next state object according to the state
	 * machine is returned
	 * 
	 * @return
	 */
	IState getNextStateObject();

	/**
	 * in this method everything should be executed what there is to do in this
	 * state
	 * @throws OpenemsNamedException 
	 */
	void act() throws OpenemsNamedException;

}
