package io.openems.edge.ess.mr.gridcon.ongrid.state;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.ess.mr.gridcon.EssGridcon;
import io.openems.edge.ess.mr.gridcon.IState;
import io.openems.edge.ess.mr.gridcon.State;
import io.openems.edge.ess.mr.gridcon.enums.ParameterSet;

public class Run extends BaseState implements State {

	private final Logger log = LoggerFactory.getLogger(Run.class);

	private boolean enableIPU1 = false;
	private boolean enableIPU2 = false;
	private boolean enableIPU3 = false;
	private ParameterSet parameterSet = ParameterSet.SET_1;
	
	public Run(EssGridcon gridconPCS, boolean enableIPU1, boolean enableIPU2, boolean enableIPU3, ParameterSet parameterSet) {
		super(gridconPCS);
		this.enableIPU1 = enableIPU1;
		this.enableIPU2 = enableIPU2;
		this.enableIPU3 = enableIPU3;
		this.parameterSet = parameterSet;
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
		return io.openems.edge.ess.mr.gridcon.ongrid.State.RUN;
	}

	

	@Override
	public void act() {
		log.info("Set all parameters to gridcon!");
		
		if (isLinkVoltageTooLow()) {
			gridconPCS.stopSystem();
		} else {
			gridconPCS.runSystem();
		}
		
	}

	private void stopSystem() {
		// TODO Auto-generated method stub
		
	}

	private boolean isLinkVoltageTooLow() {
		// TODO Auto-generated method stub
		return false;
	}

	
}
