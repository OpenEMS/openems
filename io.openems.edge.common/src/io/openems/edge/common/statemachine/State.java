package io.openems.edge.common.statemachine;

import io.openems.common.types.OptionsEnum;

public interface State<O, C> extends OptionsEnum {

	public StateHandler<O, C> getHandler();

}
