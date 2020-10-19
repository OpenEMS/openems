package io.openems.edge.ess.mr.gridcon.state.gridconstate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.ess.mr.gridcon.GridconPcs;
import io.openems.edge.ess.mr.gridcon.GridconPcsImpl;
import io.openems.edge.ess.mr.gridcon.Helper;
import io.openems.edge.ess.mr.gridcon.IState;
import io.openems.edge.ess.mr.gridcon.StateObject;
import io.openems.edge.ess.mr.gridcon.enums.Mode;
import io.openems.edge.ess.mr.gridcon.enums.PControlMode;
import io.openems.edge.ess.mr.gridcon.enums.ParameterSet;

public class Run extends BaseState implements StateObject {

	private final Logger log = LoggerFactory.getLogger(Run.class);
	private boolean enableIpu1;
	private boolean enableIpu2;
	private boolean enableIpu3;
	private float offsetCurrent;
	private ParameterSet parameterSet;

	public Run(ComponentManager manager, String gridconPcsId, String b1Id, String b2Id, String b3Id, boolean enableIpu1,
			boolean enableIpu2, boolean enableIpu3, ParameterSet parameterSet, String hardRestartRelayAdress,
			float offsetCurrent) {
		super(manager, gridconPcsId, b1Id, b2Id, b3Id, hardRestartRelayAdress);
		this.enableIpu1 = enableIpu1;
		this.enableIpu2 = enableIpu2;
		this.enableIpu3 = enableIpu3;
		this.parameterSet = parameterSet;
		this.offsetCurrent = offsetCurrent;
	}

	@Override
	public IState getState() {
		return io.openems.edge.ess.mr.gridcon.state.gridconstate.GridconState.RUN;
	}

	@Override
	public IState getNextState() {
		// According to the state machine the next state can only be ERROR, RUN
		if (isNextStateUndefined()) {
			return io.openems.edge.ess.mr.gridcon.state.gridconstate.GridconState.UNDEFINED;
		}
		if (isNextStateError()) {
			return io.openems.edge.ess.mr.gridcon.state.gridconstate.GridconState.ERROR;
		}

		if (isNextStateStopped()) {
			return io.openems.edge.ess.mr.gridcon.state.gridconstate.GridconState.STOPPED;
		}

		return io.openems.edge.ess.mr.gridcon.state.gridconstate.GridconState.RUN;
	}

	@Override
	public void act() {
		log.info("run() -> Set all parameters to gridcon!");

		// sometimes link voltage can be too low unrecognized by gridcon, i.e. no error
		// message
		// in case of that, restart the system, but this should be detected by isError()
		// function

		checkBatteries();
		setRunParameters();
		setStringWeighting();
		setOffsetCurrent();
		setStringControlMode();
		setDateAndTime();
		try {
			getGridconPcs().doWriteTasks();
		} catch (OpenemsNamedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void checkBatteries() {
		if (getBattery1() != null) {
			if (!Helper.isRunning(getBattery1()) && !Helper.isError(getBattery1())) {
				Helper.startBattery(getBattery1());
			}
		}
		if (getBattery2() != null) {
			if (!Helper.isRunning(getBattery2()) && !Helper.isError(getBattery2())) {
				Helper.startBattery(getBattery2());
			}
		}
		if (getBattery3() != null) {
			if (!Helper.isRunning(getBattery3()) && !Helper.isError(getBattery3())) {
				Helper.startBattery(getBattery3());
			}
		}
	}

	private void setOffsetCurrent() {
		if (hasBattery1HighestCellVoltage()) {
			getGridconPcs().setIRefStringA(offsetCurrent);
			getGridconPcs().setIRefStringB(0f);
			getGridconPcs().setIRefStringC(0f);
		}
		if (hasBattery2HighestCellVoltage()) {
			getGridconPcs().setIRefStringA(0f);
			getGridconPcs().setIRefStringB(offsetCurrent);
			getGridconPcs().setIRefStringC(0f);
		}
		if (hasBattery3HighestCellVoltage()) {
			getGridconPcs().setIRefStringA(0f);
			getGridconPcs().setIRefStringB(0f);
			getGridconPcs().setIRefStringC(offsetCurrent);
		}
	}

	private boolean hasBattery1HighestCellVoltage() {
		if (getBattery1() == null) { // battery is not existing --> cannot have highest cell voltage
			return false;
		}

		if (getBattery2() == null && getBattery3() == null) { // only this battery exists --> must have highest cell voltage
			return true;
		}

		if (getBattery2() == null) { // only battery one and three are existing
			return getMaxCellVoltage(getBattery1()) >= getMaxCellVoltage(getBattery3());
		}

		if (getBattery3() == null) { // only battery one and two are existing
			return getMaxCellVoltage(getBattery1()) >= getMaxCellVoltage(getBattery2());
		}

		return getMaxCellVoltage(getBattery1()) >= Math.max(getMaxCellVoltage(getBattery2()),
				getMaxCellVoltage(getBattery3()));
	}

	private boolean hasBattery2HighestCellVoltage() {
		if (getBattery2() == null) { // battery is not existing --> cannot have highest cell voltage
			return false;
		}

		if (getBattery1() == null && getBattery3() == null) { // only this battery exists --> must have highest cell voltage
			return true;
		}

		if (getBattery1() == null) { // only battery two and three are existing
			return getMaxCellVoltage(getBattery2()) >= getMaxCellVoltage(getBattery3());
		}

		if (getBattery3() == null) { // only battery one and two are existing
			return getMaxCellVoltage(getBattery2()) >= getMaxCellVoltage(getBattery1());
		}

		return getMaxCellVoltage(getBattery2()) >= Math.max(getMaxCellVoltage(getBattery1()),
				getMaxCellVoltage(getBattery3()));
	}

	private boolean hasBattery3HighestCellVoltage() {
		if (getBattery3() == null) { // battery is not existing --> cannot have highest cell voltage
			return false;
		}

		if (getBattery1() == null && getBattery2() == null) { // only this battery exists --> must have highest cell voltage
			return true;
		}

		if (getBattery2() == null) { // only battery one and three are existing
			return getMaxCellVoltage(getBattery3()) >= getMaxCellVoltage(getBattery1());
		}

		if (getBattery1() == null) { // only battery two and three are existing
			return getMaxCellVoltage(getBattery3()) >= getMaxCellVoltage(getBattery2());
		}

		return getMaxCellVoltage(getBattery3()) >= Math.max(getMaxCellVoltage(getBattery1()),
				getMaxCellVoltage(getBattery2()));
	}

	private int getMaxCellVoltage(Battery battery) {
		return battery.getMaxCellVoltage().orElse(Integer.MIN_VALUE);
	}

	private void setRunParameters() {
		getGridconPcs().setEnableIpu1(enableIpu1);
		getGridconPcs().setEnableIpu2(enableIpu2);
		getGridconPcs().setEnableIpu3(enableIpu3);

		// Enable DC DC
		getGridconPcs().enableDcDc();
		getGridconPcs().setDcLinkVoltage(GridconPcs.DC_LINK_VOLTAGE_SETPOINT);

		getGridconPcs().setSyncApproval(true);
		getGridconPcs().setBlackStartApproval(false);
		getGridconPcs().setModeSelection(Mode.CURRENT_CONTROL);
		getGridconPcs().setParameterSet(parameterSet);
		getGridconPcs().setU0(BaseState.ONLY_ON_GRID_VOLTAGE_FACTOR);
		getGridconPcs().setF0(BaseState.ONLY_ON_GRID_FREQUENCY_FACTOR);
		getGridconPcs().setPControlMode(PControlMode.ACTIVE_POWER_CONTROL);
		getGridconPcs().setQLimit(GridconPcs.Q_LIMIT);

		float maxPower = GridconPcsImpl.MAX_POWER_PER_INVERTER;
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
