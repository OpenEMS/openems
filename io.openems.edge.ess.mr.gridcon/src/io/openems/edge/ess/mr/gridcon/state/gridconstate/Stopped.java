package io.openems.edge.ess.mr.gridcon.state.gridconstate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.ess.mr.gridcon.GridconPcs;
import io.openems.edge.ess.mr.gridcon.GridconSettings;
import io.openems.edge.ess.mr.gridcon.IState;
import io.openems.edge.ess.mr.gridcon.enums.PControlMode;

public class Stopped extends BaseState {

	private final Logger log = LoggerFactory.getLogger(Stopped.class);

	private boolean enableIpu1;
	private boolean enableIpu2;
	private boolean enableIpu3;
//	private ParameterSet parameterSet;

	public Stopped(ComponentManager manager, String gridconPcsId, String b1Id, String b2Id, String b3Id,
			boolean enableIpu1, boolean enableIpu2, boolean enableIpu3, // ParameterSet parameterSet,
			String hardRestartRelayAdress) {
		super(manager, gridconPcsId, b1Id, b2Id, b3Id, hardRestartRelayAdress);
		this.enableIpu1 = enableIpu1;
		this.enableIpu2 = enableIpu2;
		this.enableIpu3 = enableIpu3;
//		this.parameterSet = parameterSet;
	}

	@Override
	public IState getState() {
		return io.openems.edge.ess.mr.gridcon.state.gridconstate.GridconState.STOPPED;
	}

	@Override
	public IState getNextState() {
		// According to the state machine the next state can only be STOPPED, ERROR, RUN
		if (isNextStateUndefined()) {
			return io.openems.edge.ess.mr.gridcon.state.gridconstate.GridconState.UNDEFINED;
		}
		if (isNextStateError()) {
			return io.openems.edge.ess.mr.gridcon.state.gridconstate.GridconState.ERROR;
		}
		if (isBatteriesStarted() && getGridconPcs().isRunning()) {
			return io.openems.edge.ess.mr.gridcon.state.gridconstate.GridconState.RUN;
		}

		return io.openems.edge.ess.mr.gridcon.state.gridconstate.GridconState.STOPPED;
	}

	@Override
	public void act(GridconSettings settings) {
		log.info("Start batteries and gridcon!");

		startSystem(settings);
		setStringWeighting();
		setStringControlMode();
		setDateAndTime();

		try {
			getGridconPcs().doWriteTasks();
		} catch (OpenemsNamedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void startSystem(GridconSettings settings) {

		if (!isBatteriesStarted()) {
			System.out.println("Batteries are not started, start batteries and keep system stopped!");
			startBatteries();
			keepSystemStopped(settings);
			return;
		}

		if (isBatteriesStarted()) {

			if (!getGridconPcs().isDcDcStarted()) {
				startDcDc(settings);
				return;
			}
			enableIpus(settings);
		}
	}

	private void keepSystemStopped(GridconSettings settings) {
		getGridconPcs().setEnableIpu1(false);
		getGridconPcs().setEnableIpu2(false);
		getGridconPcs().setEnableIpu3(false);
		getGridconPcs().disableDcDc();

		getGridconPcs().setStop(true);
		getGridconPcs().setPlay(false);
		getGridconPcs().setAcknowledge(false);

		getGridconPcs().setMode(settings.getMode());
		getGridconPcs().setU0(settings.getU0());
		getGridconPcs().setF0(settings.getF0());
		getGridconPcs().setPControlMode(PControlMode.ACTIVE_POWER_CONTROL);
		getGridconPcs().setQLimit(GridconPcs.Q_LIMIT);
		getGridconPcs().setDcLinkVoltage(GridconPcs.DC_LINK_VOLTAGE_SETPOINT);

//		getGridconPcs().setParameterSet(parameterSet);
		float maxPower = GridconPcs.MAX_POWER_PER_INVERTER;
		if (enableIpu1) {
			getGridconPcs().setPMaxChargeIpu1(maxPower);
			getGridconPcs().setPMaxDischargeIpu1(-maxPower);
		}
		if (enableIpu2) {
			getGridconPcs().setPMaxChargeIpu2(maxPower);
			getGridconPcs().setPMaxDischargeIpu2(-maxPower);
		}
		if (enableIpu3) {
			getGridconPcs().setPMaxChargeIpu3(maxPower);
			getGridconPcs().setPMaxDischargeIpu3(-maxPower);
		}
	}

	private void enableIpus(GridconSettings settings) {
		getGridconPcs().setEnableIpu1(enableIpu1);
		getGridconPcs().setEnableIpu2(enableIpu2);
		getGridconPcs().setEnableIpu3(enableIpu3);
		getGridconPcs().enableDcDc();
		getGridconPcs().setStop(false);
		getGridconPcs().setPlay(false);
		getGridconPcs().setAcknowledge(false);

		getGridconPcs().setMode(settings.getMode());
		getGridconPcs().setU0(settings.getU0());
		getGridconPcs().setF0(settings.getF0());
		getGridconPcs().setPControlMode(PControlMode.ACTIVE_POWER_CONTROL);
		getGridconPcs().setQLimit(GridconPcs.Q_LIMIT);
		getGridconPcs().setDcLinkVoltage(GridconPcs.DC_LINK_VOLTAGE_SETPOINT);

//		getGridconPcs().setParameterSet(parameterSet);
		float maxPower = GridconPcs.MAX_POWER_PER_INVERTER;
		if (enableIpu1) {
			getGridconPcs().setPMaxChargeIpu1(maxPower);
			getGridconPcs().setPMaxDischargeIpu1(-maxPower);
		}
		if (enableIpu2) {
			getGridconPcs().setPMaxChargeIpu2(maxPower);
			getGridconPcs().setPMaxDischargeIpu2(-maxPower);
		}
		if (enableIpu3) {
			getGridconPcs().setPMaxChargeIpu3(maxPower);
			getGridconPcs().setPMaxDischargeIpu3(-maxPower);
		}
	}

	private void startDcDc(GridconSettings settings) {
		getGridconPcs().setEnableIpu1(false);
		getGridconPcs().setEnableIpu2(false);
		getGridconPcs().setEnableIpu3(false);
		getGridconPcs().enableDcDc();
		getGridconPcs().setStop(false);
		getGridconPcs().setPlay(true);
		getGridconPcs().setAcknowledge(false);

		getGridconPcs().setMode(settings.getMode());
		getGridconPcs().setU0(settings.getU0());
		getGridconPcs().setF0(settings.getF0());
		getGridconPcs().setPControlMode(PControlMode.ACTIVE_POWER_CONTROL);
		getGridconPcs().setQLimit(GridconPcs.Q_LIMIT);
		getGridconPcs().setDcLinkVoltage(GridconPcs.DC_LINK_VOLTAGE_SETPOINT);

//		getGridconPcs().setParameterSet(parameterSet);
		float maxPower = GridconPcs.MAX_POWER_PER_INVERTER;
		if (enableIpu1) {
			getGridconPcs().setPMaxChargeIpu1(maxPower);
			getGridconPcs().setPMaxDischargeIpu1(-maxPower);
		}
		if (enableIpu2) {
			getGridconPcs().setPMaxChargeIpu2(maxPower);
			getGridconPcs().setPMaxDischargeIpu2(-maxPower);
		}
		if (enableIpu3) {
			getGridconPcs().setPMaxChargeIpu3(maxPower);
			getGridconPcs().setPMaxDischargeIpu3(-maxPower);
		}
	}

}
