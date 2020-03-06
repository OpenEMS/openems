package io.openems.edge.ess.mr.gridcon.onoffgrid.state;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.ess.mr.gridcon.GridconPCS;
import io.openems.edge.ess.mr.gridcon.GridconPCSImpl;
import io.openems.edge.ess.mr.gridcon.IState;
import io.openems.edge.ess.mr.gridcon.StateObject;
import io.openems.edge.ess.mr.gridcon.enums.Mode;
import io.openems.edge.ess.mr.gridcon.enums.PControlMode;
import io.openems.edge.ess.mr.gridcon.enums.ParameterSet;

public class RunOnGrid extends BaseState implements StateObject {

	private final Logger log = LoggerFactory.getLogger(RunOnGrid.class);
	private boolean enableIPU1;
	private boolean enableIPU2;
	private boolean enableIPU3;
	private ParameterSet parameterSet;

	public RunOnGrid(ComponentManager manager, String gridconPCSId, String b1Id, String b2Id, String b3Id, boolean enableIPU1, boolean enableIPU2, boolean enableIPU3, ParameterSet parameterSet ) {
		super(manager, gridconPCSId, b1Id, b2Id, b3Id);
		this.enableIPU1 = enableIPU1;
		this.enableIPU2 = enableIPU2;
		this.enableIPU3 = enableIPU3;
		this.parameterSet = parameterSet;
	}

	@Override
	public IState getState() {
		return io.openems.edge.ess.mr.gridcon.ongrid.OnGridState.RUN;
	}

	@Override
	public IState getNextState() {
		// According to the state machine the next state can only be ERROR, RUN
		if (isNextStateUndefined()) {
			return io.openems.edge.ess.mr.gridcon.ongrid.OnGridState.UNDEFINED;
		}
		if (isNextStateError()) {
			return io.openems.edge.ess.mr.gridcon.ongrid.OnGridState.ERROR;
		}
		
		if (isNextStateStopped()) {
			return io.openems.edge.ess.mr.gridcon.ongrid.OnGridState.STOPPED;
		}
		
		return io.openems.edge.ess.mr.gridcon.ongrid.OnGridState.RUN;
	}

	

	@Override
	public void act() {
		log.info("Set all parameters to gridcon!");
		
		// sometimes link voltage can be too low unrecognized by gridcon, i.e. no error message
		// in case of that, restart the system, but this should be detected by isError() function
		
		
		
		setRunParameters();
		setStringWeighting();
		setStringControlMode();
		setDateAndTime();
		try {
			getGridconPCS().doWriteTasks();
		} catch (OpenemsNamedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void setRunParameters() {
		getGridconPCS().setEnableIPU1(enableIPU1);
		getGridconPCS().setEnableIPU2(enableIPU2);
		getGridconPCS().setEnableIPU3(enableIPU3);

		// Enable DC DC
		getGridconPCS().enableDCDC();
		getGridconPCS().setDcLinkVoltage(GridconPCS.DC_LINK_VOLTAGE_SETPOINT);
		
		getGridconPCS().setSyncApproval(true);
		getGridconPCS().setBlackStartApproval(false);
		getGridconPCS().setModeSelection(Mode.CURRENT_CONTROL);
		getGridconPCS().setParameterSet(parameterSet);
		getGridconPCS().setU0(BaseState.ONLY_ON_GRID_VOLTAGE_FACTOR);
		getGridconPCS().setF0(BaseState.ONLY_ON_GRID_FREQUENCY_FACTOR);
		getGridconPCS().setPControlMode(PControlMode.ACTIVE_POWER_CONTROL);
		getGridconPCS().setQLimit(GridconPCS.Q_LIMIT);
		
		float maxPower = GridconPCSImpl.MAX_POWER_PER_INVERTER;
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
