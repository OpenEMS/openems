package io.openems.edge.controller.ess.limitdischargecellvoltage.state;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.controller.ess.limitdischargecellvoltage.IState;
import io.openems.edge.controller.ess.limitdischargecellvoltage.State;
import io.openems.edge.ess.api.ManagedSymmetricEss;

public class Check extends BaseState implements IState {

	private final Logger log = LoggerFactory.getLogger(Undefined.class);

	private int deltaSoC;
	
	public Check(ManagedSymmetricEss ess, int deltaSoC) {
		super(ess);
		this.deltaSoC = deltaSoC;
	}

	@Override
	public State getState() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public State getNextState() {
		// According to the state machine the next states can be:
		// UNDEFINED: if at least one value is not available 
		// CHECK: the soc has not increased enough
		// NORMAL: soc has increased enough
		// LIMIT: cell voltage/temperature or soc are under limits
		return null;
	}

	@Override
	public void act() throws OpenemsNamedException {
		log.info("act");
	}

}
