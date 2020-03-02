package io.openems.edge.ess.mr.gridcon.ongrid.state;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.ess.mr.gridcon.GridconPCS;
import io.openems.edge.ess.mr.gridcon.IState;
import io.openems.edge.ess.mr.gridcon.State;
import io.openems.edge.ess.mr.gridcon.battery.SoltaroBattery;
import io.openems.edge.ess.mr.gridcon.enums.Mode;
import io.openems.edge.ess.mr.gridcon.enums.PControlMode;
import io.openems.edge.ess.mr.gridcon.enums.ParameterSet;

public class Stopped extends BaseState implements State {

	private final Logger log = LoggerFactory.getLogger(Stopped.class);

	private boolean enableIPU1;
	private boolean enableIPU2;
	private boolean enableIPU3;
	private ParameterSet parameterSet;
	
	public Stopped(GridconPCS gridconPCS, SoltaroBattery b1, SoltaroBattery b2, SoltaroBattery b3, boolean enableIPU1, boolean enableIPU2, boolean enableIPU3, ParameterSet parameterSet ) {
		super(gridconPCS, b1, b2, b3);
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
		// According to the state machine the next state can only be STOPPED, ERROR, RUN
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
		setStringWeighting();
		setStringControlMode();
		setDateAndTime();
	}

	private void startSystem() {
		
		if (!isBatteriesStarted()) {
			System.out.println("Batteries are not started, start batteries and keep system stopped!");
			startBatteries();
			keepSystemStopped();
			return;
		}
				
		if (isBatteriesStarted()) {
								
			if (!gridconPCS.isDcDcStarted()) {				
				startDcDc();
				return;
			}
			enableIPUs();
		}
	}

	private void keepSystemStopped() {		
			gridconPCS.setEnableIPU1(false);
			gridconPCS.setEnableIPU2(false);
			gridconPCS.setEnableIPU3(false);
			gridconPCS.disableDCDC();
			
			gridconPCS.setStop(true);
			gridconPCS.setPlay(false);
			gridconPCS.setAcknowledge(false);
			
			gridconPCS.setSyncApproval(true);
			gridconPCS.setBlackStartApproval(false);
			gridconPCS.setModeSelection(Mode.CURRENT_CONTROL);
			gridconPCS.setU0(BaseState.ONLY_ON_GRID_VOLTAGE_FACTOR);
			gridconPCS.setF0(BaseState.ONLY_ON_GRID_FREQUENCY_FACTOR);
			gridconPCS.setPControlMode(PControlMode.ACTIVE_POWER_CONTROL);
			gridconPCS.setQLimit(GridconPCS.Q_LIMIT);
			gridconPCS.setDcLinkVoltage(GridconPCS.DC_LINK_VOLTAGE_SETPOINT);
			
			gridconPCS.setParameterSet(parameterSet);				
			float maxPower = GridconPCS.MAX_POWER_PER_INVERTER;
			if (enableIPU1) {
				gridconPCS.setPMaxChargeIPU1(maxPower);
				gridconPCS.setPMaxDischargeIPU1(-maxPower);
			}
			if (enableIPU2) {
				gridconPCS.setPMaxChargeIPU2(maxPower);
				gridconPCS.setPMaxDischargeIPU2(-maxPower);
			}
			if (enableIPU3) {
				gridconPCS.setPMaxChargeIPU3(maxPower);
				gridconPCS.setPMaxDischargeIPU3(-maxPower);
			}		
	}

	
	private void enableIPUs() {		
		gridconPCS.setEnableIPU1(enableIPU1);
		gridconPCS.setEnableIPU2(enableIPU2);
		gridconPCS.setEnableIPU3(enableIPU3);
		gridconPCS.enableDCDC();
		gridconPCS.setStop(false);
		gridconPCS.setPlay(false); 
		gridconPCS.setAcknowledge(false);
		
		gridconPCS.setSyncApproval(true);
		gridconPCS.setBlackStartApproval(false);
		gridconPCS.setModeSelection(Mode.CURRENT_CONTROL);
		gridconPCS.setU0(BaseState.ONLY_ON_GRID_VOLTAGE_FACTOR);
		gridconPCS.setF0(BaseState.ONLY_ON_GRID_FREQUENCY_FACTOR);
		gridconPCS.setPControlMode(PControlMode.ACTIVE_POWER_CONTROL);
		gridconPCS.setQLimit(GridconPCS.Q_LIMIT);
		gridconPCS.setDcLinkVoltage(GridconPCS.DC_LINK_VOLTAGE_SETPOINT);
		
		gridconPCS.setParameterSet(parameterSet);				
		float maxPower = GridconPCS.MAX_POWER_PER_INVERTER;
		if (enableIPU1) {
			gridconPCS.setPMaxChargeIPU1(maxPower);
			gridconPCS.setPMaxDischargeIPU1(-maxPower);
		}
		if (enableIPU2) {
			gridconPCS.setPMaxChargeIPU2(maxPower);
			gridconPCS.setPMaxDischargeIPU2(-maxPower);
		}
		if (enableIPU3) {
			gridconPCS.setPMaxChargeIPU3(maxPower);
			gridconPCS.setPMaxDischargeIPU3(-maxPower);
		}
	}
	
	private void startDcDc() {
		gridconPCS.setEnableIPU1(false);
		gridconPCS.setEnableIPU2(false);
		gridconPCS.setEnableIPU3(false);
		gridconPCS.enableDCDC();
		gridconPCS.setStop(false);
		gridconPCS.setPlay(true); 
		gridconPCS.setAcknowledge(false);
		
		gridconPCS.setSyncApproval(true);
		gridconPCS.setBlackStartApproval(false);
		gridconPCS.setModeSelection(Mode.CURRENT_CONTROL);
		gridconPCS.setU0(BaseState.ONLY_ON_GRID_VOLTAGE_FACTOR);
		gridconPCS.setF0(BaseState.ONLY_ON_GRID_FREQUENCY_FACTOR);
		gridconPCS.setPControlMode(PControlMode.ACTIVE_POWER_CONTROL);
		gridconPCS.setQLimit(GridconPCS.Q_LIMIT);
		gridconPCS.setDcLinkVoltage(GridconPCS.DC_LINK_VOLTAGE_SETPOINT);
		
		gridconPCS.setParameterSet(parameterSet);				
		float maxPower = GridconPCS.MAX_POWER_PER_INVERTER;
		if (enableIPU1) {
			gridconPCS.setPMaxChargeIPU1(maxPower);
			gridconPCS.setPMaxDischargeIPU1(-maxPower);
		}
		if (enableIPU2) {
			gridconPCS.setPMaxChargeIPU2(maxPower);
			gridconPCS.setPMaxDischargeIPU2(-maxPower);
		}
		if (enableIPU3) {
			gridconPCS.setPMaxChargeIPU3(maxPower);
			gridconPCS.setPMaxDischargeIPU3(-maxPower);
		}	
	}

}
