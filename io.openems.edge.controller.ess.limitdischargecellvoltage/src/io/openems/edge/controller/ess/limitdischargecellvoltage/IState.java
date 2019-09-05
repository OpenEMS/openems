package io.openems.edge.controller.ess.limitdischargecellvoltage;

public interface IState {

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
	 */
	void act();

}
