package io.openems.edge.ess.mr.gridcon.ongrid.state;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.ess.mr.gridcon.EssGridcon;
import io.openems.edge.ess.mr.gridcon.IState;
import io.openems.edge.ess.mr.gridcon.State;
import io.openems.edge.ess.mr.gridcon.battery.SoltaroBattery;

public class Run extends BaseState implements State {

	private final Logger log = LoggerFactory.getLogger(Run.class);

	public Run(EssGridcon gridconPCS, SoltaroBattery b1, SoltaroBattery b2, SoltaroBattery b3) {
		super(gridconPCS, b1, b2, b3);
	}

	@Override
	public IState getState() {
		return io.openems.edge.ess.mr.gridcon.ongrid.State.RUN;
	}

	@Override
	public IState getNextState() {
		// According to the state machine the next state can only be Stopped, ERROR, RUN
		if (isNextStateUndefined()) {
			return io.openems.edge.ess.mr.gridcon.ongrid.State.UNDEFINED;
		}
		if (isNextStateError()) {
			return io.openems.edge.ess.mr.gridcon.ongrid.State.ERROR;
		}
		
		if (isNextStateStopped()) {
			return io.openems.edge.ess.mr.gridcon.ongrid.State.STOPPED;
		}
		
		return io.openems.edge.ess.mr.gridcon.ongrid.State.RUN;
	}

	

	@Override
	public void act() {
		log.info("Set all parameters to gridcon!");
		
		// sometimes link voltage can be too low unrecognized by gridcon, i.e. no error message
		// in case of that, restart the system, but this should be detected by isError() function
		gridconPCS.runSystem();
		setStringWeighting();
		setStringControlMode();
		setDateAndTime();

	}

	
}
