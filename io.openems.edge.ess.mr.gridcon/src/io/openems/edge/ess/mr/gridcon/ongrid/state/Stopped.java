package io.openems.edge.ess.mr.gridcon.ongrid.state;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.ess.mr.gridcon.EssGridcon;
import io.openems.edge.ess.mr.gridcon.GridconPCS;
import io.openems.edge.ess.mr.gridcon.GridconPCSImpl;
import io.openems.edge.ess.mr.gridcon.IState;
import io.openems.edge.ess.mr.gridcon.State;
import io.openems.edge.ess.mr.gridcon.enums.Mode;
import io.openems.edge.ess.mr.gridcon.enums.PControlMode;
import io.openems.edge.ess.mr.gridcon.enums.ParameterSet;

public class Stopped extends BaseState implements State {

	private final Logger log = LoggerFactory.getLogger(Stopped.class);

	private boolean enableIPU1 = false;
	private boolean enableIPU2 = false;
	private boolean enableIPU3 = false;
	private ParameterSet parameterSet = ParameterSet.SET_1;
	
	public Stopped(EssGridcon gridconPCS, boolean enableIPU1, boolean enableIPU2, boolean enableIPU3, ParameterSet parameterSet) {
		super(gridconPCS);
		this.enableIPU1 = enableIPU1;
		this.enableIPU2 = enableIPU2;
		this.enableIPU3 = enableIPU3;
		this.parameterSet = parameterSet;
	}

	@Override
	public IState getState() {
		return io.openems.edge.ess.mr.gridcon.ongrid.State.STOPPED;
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
		if (isBatteriesStarted() && gridconPCS.isRunning()) {
			return io.openems.edge.ess.mr.gridcon.ongrid.State.RUN;
		}
		
		return io.openems.edge.ess.mr.gridcon.ongrid.State.STOPPED;
	}

	

	@Override
	public void act() {
		log.info("Start batteries and gridcon!");

		startSystem();
		
	}

	private void startSystem() {
		if (!isBatteriesStarted()) {
			startBatteries();
		}
		if (isBatteriesStarted()) {
			gridconPCS.start();
		}
	}

	private void startBatteries() {
//TODO		
	}

	private boolean isBatteriesStarted() {
//TODO
		return true;
	}

	
}
