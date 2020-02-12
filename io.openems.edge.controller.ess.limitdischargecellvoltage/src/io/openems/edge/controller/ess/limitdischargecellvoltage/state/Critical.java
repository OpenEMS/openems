package io.openems.edge.controller.ess.limitdischargecellvoltage.state;

import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.controller.ess.limitdischargecellvoltage.Config;
import io.openems.edge.controller.ess.limitdischargecellvoltage.IState;
import io.openems.edge.controller.ess.limitdischargecellvoltage.State;

public class Critical implements IState {

	private ComponentManager componentManager;
	private Config config;

	public Critical(ComponentManager componentManager, Config config) {
		this.componentManager = componentManager;
		this.config = config;
	}

	@Override
	public State getState() {
		return State.CRITICAL;
	}

	@Override
	public IState getNextStateObject() {
		// According to the state machine the next state is always charge
		return new Charge(this.componentManager, this.config);
	}

	@Override
	public void act() {
		// nothing to do
	}
}
