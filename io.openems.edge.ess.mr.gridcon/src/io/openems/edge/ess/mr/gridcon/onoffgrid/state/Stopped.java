package io.openems.edge.ess.mr.gridcon.onoffgrid.state;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.ess.mr.gridcon.GridconPCS;
import io.openems.edge.ess.mr.gridcon.IState;
import io.openems.edge.ess.mr.gridcon.StateObject;
import io.openems.edge.ess.mr.gridcon.enums.Mode;
import io.openems.edge.ess.mr.gridcon.enums.PControlMode;
import io.openems.edge.ess.mr.gridcon.enums.ParameterSet;

public class Stopped extends BaseState implements StateObject {

	private final Logger log = LoggerFactory.getLogger(Stopped.class);

	private boolean enableIPU1;
	private boolean enableIPU2;
	private boolean enableIPU3;
	private ParameterSet parameterSet;
	private float targetFrequency;
	
	public Stopped(ComponentManager manager, String gridconPCSId, String b1Id, String b2Id, String b3Id, boolean enableIPU1, boolean enableIPU2, boolean enableIPU3, ParameterSet parameterSet,
			String inputNA1, String inputNA2, String inputSyncBridge, String outputSyncBridge, float targetFrequency, String meterId) {
		super(manager, gridconPCSId, b1Id, b2Id, b3Id, inputNA1, inputNA2, inputSyncBridge, outputSyncBridge, meterId);
		this.enableIPU1 = enableIPU1;
		this.enableIPU2 = enableIPU2;
		this.enableIPU3 = enableIPU3;
		this.parameterSet = parameterSet;
		this.targetFrequency = targetFrequency;
	}

	@Override
	public IState getState() {
		return io.openems.edge.ess.mr.gridcon.onoffgrid.OnOffGridState.STOPPED;
	}

	@Override
	public IState getNextState() {
		// According to the state machine the next state can only be STOPPED, ERROR, RUN
		if (isNextStateUndefined()) {
			return io.openems.edge.ess.mr.gridcon.onoffgrid.OnOffGridState.UNDEFINED;
		}
		if (isNextStateError()) {
			return io.openems.edge.ess.mr.gridcon.onoffgrid.OnOffGridState.ERROR;
		}
		if (isBatteriesStarted() && getGridconPCS().isRunning()) {			
			return io.openems.edge.ess.mr.gridcon.onoffgrid.OnOffGridState.RUN_ONGRID;
		}
		
		return io.openems.edge.ess.mr.gridcon.onoffgrid.OnOffGridState.STOPPED;
	}

	@Override
	public void act() {
		log.info("Start batteries and gridcon!");

		startSystem();
		setStringWeighting();
		setStringControlMode();
		setDateAndTime();
		setSyncBridge(false);
		
		try {
			getGridconPCS().doWriteTasks();
		} catch (OpenemsNamedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void startSystem() {
		
		if (!isBatteriesStarted()) {
			System.out.println("Batteries are not started, start batteries and keep system stopped!");
			startBatteries();
			keepSystemStopped();
			return;
		}
				
		if (isBatteriesStarted()) {
								
			if (!getGridconPCS().isDcDcStarted()) {				
				startDcDc();
				return;
			}
			enableIPUs();
		}
	}

	private void keepSystemStopped() {		
			getGridconPCS().setEnableIPU1(false);
			getGridconPCS().setEnableIPU2(false);
			getGridconPCS().setEnableIPU3(false);
			getGridconPCS().disableDCDC();
			
			getGridconPCS().setStop(true);
			getGridconPCS().setPlay(false);
			getGridconPCS().setAcknowledge(false);
			
			getGridconPCS().setSyncApproval(true);
			getGridconPCS().setBlackStartApproval(false);
			getGridconPCS().setModeSelection(Mode.CURRENT_CONTROL);
			getGridconPCS().setU0(BaseState.ONOFF_GRID_VOLTAGE_FACTOR);
			getGridconPCS().setF0(targetFrequency);
			getGridconPCS().setPControlMode(PControlMode.ACTIVE_POWER_CONTROL);
			getGridconPCS().setQLimit(GridconPCS.Q_LIMIT);
			getGridconPCS().setDcLinkVoltage(GridconPCS.DC_LINK_VOLTAGE_SETPOINT);
			
			getGridconPCS().setParameterSet(parameterSet);				
			float maxPower = GridconPCS.MAX_POWER_PER_INVERTER;
			if (enableIPU1) {
				getGridconPCS().setPMaxChargeIPU1(maxPower);
				getGridconPCS().setPMaxDischargeIPU1(-maxPower);
			}
			if (enableIPU2) {
				getGridconPCS().setPMaxChargeIPU2(maxPower);
				getGridconPCS().setPMaxDischargeIPU2(-maxPower);
			}
			if (enableIPU3) {
				getGridconPCS().setPMaxChargeIPU3(maxPower);
				getGridconPCS().setPMaxDischargeIPU3(-maxPower);
			}		
	}

	
	private void enableIPUs() {		
		getGridconPCS().setEnableIPU1(enableIPU1);
		getGridconPCS().setEnableIPU2(enableIPU2);
		getGridconPCS().setEnableIPU3(enableIPU3);
		getGridconPCS().enableDCDC();
		getGridconPCS().setStop(false);
		getGridconPCS().setPlay(false); 
		getGridconPCS().setAcknowledge(false);
		
		getGridconPCS().setSyncApproval(true);
		getGridconPCS().setBlackStartApproval(false);
		getGridconPCS().setModeSelection(Mode.CURRENT_CONTROL);
		getGridconPCS().setU0(BaseState.ONOFF_GRID_VOLTAGE_FACTOR);
		getGridconPCS().setF0(targetFrequency);
		getGridconPCS().setPControlMode(PControlMode.ACTIVE_POWER_CONTROL);
		getGridconPCS().setQLimit(GridconPCS.Q_LIMIT);
		getGridconPCS().setDcLinkVoltage(GridconPCS.DC_LINK_VOLTAGE_SETPOINT);
		
		getGridconPCS().setParameterSet(parameterSet);				
		float maxPower = GridconPCS.MAX_POWER_PER_INVERTER;
		if (enableIPU1) {
			getGridconPCS().setPMaxChargeIPU1(maxPower);
			getGridconPCS().setPMaxDischargeIPU1(-maxPower);
		}
		if (enableIPU2) {
			getGridconPCS().setPMaxChargeIPU2(maxPower);
			getGridconPCS().setPMaxDischargeIPU2(-maxPower);
		}
		if (enableIPU3) {
			getGridconPCS().setPMaxChargeIPU3(maxPower);
			getGridconPCS().setPMaxDischargeIPU3(-maxPower);
		}
	}
	
	private void startDcDc() {
		getGridconPCS().setEnableIPU1(false);
		getGridconPCS().setEnableIPU2(false);
		getGridconPCS().setEnableIPU3(false);
		getGridconPCS().enableDCDC();
		getGridconPCS().setStop(false);
		getGridconPCS().setPlay(true); 
		getGridconPCS().setAcknowledge(false);
		
		getGridconPCS().setSyncApproval(true);
		getGridconPCS().setBlackStartApproval(false);
		getGridconPCS().setModeSelection(Mode.CURRENT_CONTROL);
		getGridconPCS().setU0(BaseState.ONOFF_GRID_VOLTAGE_FACTOR);
		getGridconPCS().setF0(targetFrequency);
		getGridconPCS().setPControlMode(PControlMode.ACTIVE_POWER_CONTROL);
		getGridconPCS().setQLimit(GridconPCS.Q_LIMIT);
		getGridconPCS().setDcLinkVoltage(GridconPCS.DC_LINK_VOLTAGE_SETPOINT);
		
		getGridconPCS().setParameterSet(parameterSet);				
		float maxPower = GridconPCS.MAX_POWER_PER_INVERTER;
		if (enableIPU1) {
			getGridconPCS().setPMaxChargeIPU1(maxPower);
			getGridconPCS().setPMaxDischargeIPU1(-maxPower);
		}
		if (enableIPU2) {
			getGridconPCS().setPMaxChargeIPU2(maxPower);
			getGridconPCS().setPMaxDischargeIPU2(-maxPower);
		}
		if (enableIPU3) {
			getGridconPCS().setPMaxChargeIPU3(maxPower);
			getGridconPCS().setPMaxDischargeIPU3(-maxPower);
		}	
	}

}
