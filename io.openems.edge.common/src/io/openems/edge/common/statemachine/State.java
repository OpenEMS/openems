package io.openems.edge.common.statemachine;

import io.openems.common.types.OptionsEnum;

/**
 * Defines a State of a {@link StateMachine}. This interface is typicall
 * implemented by an enum.
 *
 * @param <S> the actual State type
 * @param <C> the context type
 */
public interface State<S, C> extends OptionsEnum {

	public StateHandler<S, C> getHandler();

}
