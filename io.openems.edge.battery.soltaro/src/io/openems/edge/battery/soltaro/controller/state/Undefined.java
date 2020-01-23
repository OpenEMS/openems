package io.openems.edge.battery.soltaro.controller.state;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.battery.api.Battery;
import io.openems.edge.battery.soltaro.controller.IState;
import io.openems.edge.battery.soltaro.controller.State;
import io.openems.edge.ess.api.ManagedSymmetricEss;

public class Undefined extends BaseState implements IState {

	private final Logger log = LoggerFactory.getLogger(Undefined.class);

	public Undefined(ManagedSymmetricEss ess, Battery bms) {
		super(ess, bms);
	}

	@Override
	public State getState() {
		return State.UNDEFINED;
	}

	@Override
	public State getNextState() {
		// According to the state machine the next state can only be NORMAL
		if (isNextStateUndefined()) {
			return State.UNDEFINED;
		}
	
		return State.NORMAL;
	}

	@Override
	public void act() {
		log.info("Nothing to do!");
	}
}
