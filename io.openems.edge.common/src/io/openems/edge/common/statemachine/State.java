package io.openems.edge.common.statemachine;

import io.openems.common.types.OptionsEnum;

/**
 * Defines a State of a {@link AbstractStateMachine}. This interface is
 * typically implemented by an enum.
 *
 * @param <STATE> the actual State type
 */
// CHECKSTYLE:OFF
public interface State<STATE extends State<STATE>> extends OptionsEnum {
	// CHECKSTYLE:ON

	/**
	 * Gets all the available States.
	 *
	 * <p>
	 * If used inside a 'State' enum, just implement this method using
	 *
	 * <pre>
	 * &#64;Override
	 * public State[] getStates() {
	 * 	return State.values();
	 * }
	 * </pre>
	 *
	 * @return an array of States, as provided by an enum.
	 */
	public STATE[] getStates();

}
