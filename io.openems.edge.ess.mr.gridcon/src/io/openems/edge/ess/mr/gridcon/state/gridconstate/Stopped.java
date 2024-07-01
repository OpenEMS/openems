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
	// private ParameterSet parameterSet;

	public Stopped(ComponentManager manager, String gridconPcsId, String b1Id, String b2Id, String b3Id,
			boolean enableIpu1, boolean enableIpu2, boolean enableIpu3, // ParameterSet parameterSet,
			String hardRestartRelayAdress) {
		super(manager, gridconPcsId, b1Id, b2Id, b3Id, hardRestartRelayAdress);
		this.enableIpu1 = enableIpu1;
		this.enableIpu2 = enableIpu2;
		this.enableIpu3 = enableIpu3;
		// this.parameterSet = parameterSet;
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
		this.log.info("Start batteries and gridcon!");

		this.startSystem(settings);
		this.setStringWeighting();
		this.setStringControlMode();
		this.setDateAndTime();

		try {
			this.getGridconPcs().doWriteTasks();
		} catch (OpenemsNamedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void startSystem(GridconSettings settings) {

		if (!this.isBatteriesStarted()) {
			System.out.println("Batteries are not started, start batteries and keep system stopped!");
			this.startBatteries();
			this.keepSystemStopped(settings);
			return;
		}

		if (isBatteriesStarted()) {

			if (!getGridconPcs().isDcDcStarted()) {
				this.startDcDc(settings);
				return;
			}
			this.enableIpus(settings);
		}
	}

	private void keepSystemStopped(GridconSettings settings) {
		this.getGridconPcs().setEnableIpu1(false);
		this.getGridconPcs().setEnableIpu2(false);
		this.getGridconPcs().setEnableIpu3(false);
		this.getGridconPcs().disableDcDc();

		this.getGridconPcs().setStop(true);
		this.getGridconPcs().setPlay(false);
		this.getGridconPcs().setAcknowledge(false);

		this.getGridconPcs().setMode(settings.getMode());
		this.getGridconPcs().setU0(settings.getU0());
		this.getGridconPcs().setF0(settings.getF0());
		this.getGridconPcs().setPControlMode(PControlMode.ACTIVE_POWER_CONTROL);
		this.getGridconPcs().setQLimit(GridconPcs.Q_LIMIT);
		this.getGridconPcs().setDcLinkVoltage(GridconPcs.DC_LINK_VOLTAGE_SETPOINT);

		// getGridconPcs().setParameterSet(parameterSet);
		float maxPower = GridconPcs.MAX_POWER_PER_INVERTER;
		if (this.enableIpu1) {
			this.getGridconPcs().setPMaxChargeIpu1(maxPower);
			this.getGridconPcs().setPMaxDischargeIpu1(-maxPower);
		}
		if (this.enableIpu2) {
			this.getGridconPcs().setPMaxChargeIpu2(maxPower);
			this.getGridconPcs().setPMaxDischargeIpu2(-maxPower);
		}
		if (this.enableIpu3) {
			this.getGridconPcs().setPMaxChargeIpu3(maxPower);
			this.getGridconPcs().setPMaxDischargeIpu3(-maxPower);
		}
	}

	private void enableIpus(GridconSettings settings) {
		this.getGridconPcs().setEnableIpu1(this.enableIpu1);
		this.getGridconPcs().setEnableIpu2(this.enableIpu2);
		this.getGridconPcs().setEnableIpu3(this.enableIpu3);
		this.getGridconPcs().enableDcDc();
		this.getGridconPcs().setStop(false);
		this.getGridconPcs().setPlay(false);
		this.getGridconPcs().setAcknowledge(false);

		this.getGridconPcs().setMode(settings.getMode());
		this.getGridconPcs().setU0(settings.getU0());
		this.getGridconPcs().setF0(settings.getF0());
		this.getGridconPcs().setPControlMode(PControlMode.ACTIVE_POWER_CONTROL);
		this.getGridconPcs().setQLimit(GridconPcs.Q_LIMIT);
		this.getGridconPcs().setDcLinkVoltage(GridconPcs.DC_LINK_VOLTAGE_SETPOINT);

		// getGridconPcs().setParameterSet(parameterSet);
		float maxPower = GridconPcs.MAX_POWER_PER_INVERTER;
		if (this.enableIpu1) {
			this.getGridconPcs().setPMaxChargeIpu1(maxPower);
			this.getGridconPcs().setPMaxDischargeIpu1(-maxPower);
		}
		if (this.enableIpu2) {
			this.getGridconPcs().setPMaxChargeIpu2(maxPower);
			this.getGridconPcs().setPMaxDischargeIpu2(-maxPower);
		}
		if (this.enableIpu3) {
			this.getGridconPcs().setPMaxChargeIpu3(maxPower);
			this.getGridconPcs().setPMaxDischargeIpu3(-maxPower);
		}
	}

	private void startDcDc(GridconSettings settings) {
		this.getGridconPcs().setEnableIpu1(false);
		this.getGridconPcs().setEnableIpu2(false);
		this.getGridconPcs().setEnableIpu3(false);
		this.getGridconPcs().enableDcDc();
		this.getGridconPcs().setStop(false);
		this.getGridconPcs().setPlay(true);
		this.getGridconPcs().setAcknowledge(false);

		this.getGridconPcs().setMode(settings.getMode());
		this.getGridconPcs().setU0(settings.getU0());
		this.getGridconPcs().setF0(settings.getF0());
		this.getGridconPcs().setPControlMode(PControlMode.ACTIVE_POWER_CONTROL);
		this.getGridconPcs().setQLimit(GridconPcs.Q_LIMIT);
		this.getGridconPcs().setDcLinkVoltage(GridconPcs.DC_LINK_VOLTAGE_SETPOINT);

		// getGridconPcs().setParameterSet(parameterSet);
		float maxPower = GridconPcs.MAX_POWER_PER_INVERTER;
		if (this.enableIpu1) {
			this.getGridconPcs().setPMaxChargeIpu1(maxPower);
			this.getGridconPcs().setPMaxDischargeIpu1(-maxPower);
		}
		if (this.enableIpu2) {
			this.getGridconPcs().setPMaxChargeIpu2(maxPower);
			getGridconPcs().setPMaxDischargeIpu2(-maxPower);
		}
		if (this.enableIpu3) {
			getGridconPcs().setPMaxChargeIpu3(maxPower);
			getGridconPcs().setPMaxDischargeIpu3(-maxPower);
		}
	}

}
