package io.openems.edge.ess.mr.gridcon.state.gridconstate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.ess.mr.gridcon.GridconPCS;
import io.openems.edge.ess.mr.gridcon.GridconPCSImpl;
import io.openems.edge.ess.mr.gridcon.IState;
import io.openems.edge.ess.mr.gridcon.StateObject;
import io.openems.edge.ess.mr.gridcon.enums.Mode;
import io.openems.edge.ess.mr.gridcon.enums.PControlMode;
import io.openems.edge.ess.mr.gridcon.enums.ParameterSet;

public class Run extends BaseState implements StateObject {

	private final Logger log = LoggerFactory.getLogger(Run.class);
	private boolean enableIPU1;
	private boolean enableIPU2;
	private boolean enableIPU3;
	private float offsetCurrent;
	private ParameterSet parameterSet;

	public Run(ComponentManager manager, String gridconPCSId, String b1Id, String b2Id, String b3Id, boolean enableIPU1,
			boolean enableIPU2, boolean enableIPU3, ParameterSet parameterSet, String hardRestartRelayAdress,
			float offsetCurrent) {
		super(manager, gridconPCSId, b1Id, b2Id, b3Id, hardRestartRelayAdress);
		this.enableIPU1 = enableIPU1;
		this.enableIPU2 = enableIPU2;
		this.enableIPU3 = enableIPU3;
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
			getGridconPCS().doWriteTasks();
		} catch (OpenemsNamedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void checkBatteries() {
		if (getBattery1() != null) {
			if (!getBattery1().isRunning() && !getBattery1().isError()) {
				getBattery1().start();
			}
		}
		if (getBattery2() != null) {
			if (!getBattery2().isRunning() && !getBattery2().isError()) {
				getBattery2().start();
			}
		}
		if (getBattery3() != null) {
			if (!getBattery3().isRunning() && !getBattery3().isError()) {
				getBattery3().start();
			}
		}
	}

	private void setOffsetCurrent() {

		System.out.println(" ----- Setting the offset current --------");

		// find out the battery rack with the highest cell voltage and put the offset to
		// it
		if (hasBattery1HighestCellVoltage()) {

			System.out.println("Battery 1 has the highest cell voltage, set offset current to " + offsetCurrent);

			getGridconPCS().setIRefStringA(offsetCurrent);
			getGridconPCS().setIRefStringB(0f);
			getGridconPCS().setIRefStringC(0f);
		}
		if (hasBattery2HighestCellVoltage()) {

			System.out.println("Battery 2 has the highest cell voltage, set offset current to " + offsetCurrent);

			getGridconPCS().setIRefStringA(0f);
			getGridconPCS().setIRefStringB(offsetCurrent);
			getGridconPCS().setIRefStringC(0f);
		}
		if (hasBattery3HighestCellVoltage()) {

			System.out.println("Battery 3 has the highest cell voltage, set offset current to " + offsetCurrent);

			getGridconPCS().setIRefStringA(0f);
			getGridconPCS().setIRefStringB(0f);
			getGridconPCS().setIRefStringC(offsetCurrent);
		}

		System.out.println(" ----- End of setting the offset current --------");
	}

	private boolean hasBattery1HighestCellVoltage() {
		if (getBattery1() == null) { // battery is not existing --> cannot have highest cell voltage
			return false;
		}

		if (getBattery2() == null && getBattery3() == null) { // only this battery exists --> must have highest cell
																// voltage
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

		if (getBattery1() == null && getBattery3() == null) { // only this battery exists --> must have highest cell
																// voltage
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

		if (getBattery1() == null && getBattery2() == null) { // only this battery exists --> must have highest cell
																// voltage
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
		return battery.getMaxCellVoltage().value().orElse(Integer.MIN_VALUE);
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
