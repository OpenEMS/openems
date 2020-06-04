package io.openems.edge.common.statemachine;

import io.openems.common.types.OptionsEnum;

/**
 * Defines a State of a {@link StateMachine}. This interface is typically
 * implemented by an enum.
 *
 * @param <STATE>   the actual State type
 * @param <CONTEXT> the context type
 */
public interface State<STATE extends State<STATE, CONTEXT>, CONTEXT> extends OptionsEnum {

	/**
	 * Gets the Handler for this State.
	 * 
	 * @return the {@link StateHandler}
	 */
	public StateHandler<STATE, CONTEXT> getHandler();

}
