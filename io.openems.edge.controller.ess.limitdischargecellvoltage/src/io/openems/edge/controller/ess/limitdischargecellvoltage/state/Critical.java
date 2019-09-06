package io.openems.edge.controller.ess.limitdischargecellvoltage.state;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.controller.ess.limitdischargecellvoltage.IState;
import io.openems.edge.controller.ess.limitdischargecellvoltage.State;
import io.openems.edge.controller.ess.limitdischargecellvoltage.Config;

public class Critical implements IState {

	private final Logger log = LoggerFactory.getLogger(Critical.class);

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
		log.info("Critical.getNextStateObject() --> Charge");
		return new Charge(this.componentManager, this.config);
	}

	@Override
	public void act() {
		log.info("act() --> nothing to do");
	}
}
