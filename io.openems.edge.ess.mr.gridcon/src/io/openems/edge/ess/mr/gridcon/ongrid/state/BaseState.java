package io.openems.edge.ess.mr.gridcon.ongrid.state;

import io.openems.edge.ess.mr.gridcon.EssGridcon;
import io.openems.edge.ess.mr.gridcon.State;
import io.openems.edge.ess.mr.gridcon.battery.SoltaroBattery;

public abstract class BaseState implements State {

//	private final Logger log = LoggerFactory.getLogger(BaseState.class);

	protected EssGridcon gridconPCS;
	protected SoltaroBattery battery1;
	protected SoltaroBattery battery2;
	protected SoltaroBattery battery3;
	
	public BaseState(EssGridcon gridconPCS, SoltaroBattery b1, SoltaroBattery b2, SoltaroBattery b3) {
		this.gridconPCS = gridconPCS;
		this.battery1 = b1;
		this.battery2 = b2;
		this.battery3 = b3;		
	}
	
	protected boolean isNextStateUndefined() {
		return false;
	}

	protected boolean isNextStateError() {
		if (gridconPCS != null && gridconPCS.isError()) {
			return true;
		}
		
		if (battery1 != null && battery1.isError()) {
			return true;
		}
		
		if (battery2 != null && battery2.isError()) {
			return true;
		}
		
		if (battery3 != null && battery3.isError()) {
			return true;
		}
		
		return false;
	}
	
	protected boolean isNextStateStopped() {
		return gridconPCS != null && gridconPCS.isStopped();
	}
	
	protected boolean isNextStateRunning() {
		boolean running = true;
		if (gridconPCS != null) {
			running = running && gridconPCS.isRunning();
		}
		
		if (battery1 != null) {
			running = running && battery1.isRunning();
		}
		
		if (battery2 != null) {
			running = running && battery2.isRunning();
		}
		
		if (battery3 != null) {
			running = running && battery3.isRunning();
		}
		return running;
	}

	protected void startBatteries() {
		if (battery1 != null) {
		if (!battery1.isRunning()) {
			battery1.start();
		}
		}
		if (battery2 != null) {
		if (!battery2.isRunning()) {
			battery2.start();
		}
		}
		if (battery3 != null) {
		if (!battery3.isRunning()) {
			battery3.start();
		}
		}
	}

	protected boolean isBatteriesStarted() {
		boolean running = true;
		if (battery1 != null) {
			running = running && battery1.isRunning();
		}
		if (battery2 != null) {
			running = running && battery2.isRunning();
		}
		if (battery3 != null) {
			running = running && battery3.isRunning();
		}
		return running;
	}
	
}
