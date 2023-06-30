package io.openems.edge.ess.mr.gridcon.state.gridconstate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.ess.mr.gridcon.GridconPcs;
import io.openems.edge.ess.mr.gridcon.GridconPcsImpl;
import io.openems.edge.ess.mr.gridcon.GridconSettings;
import io.openems.edge.ess.mr.gridcon.IState;
import io.openems.edge.ess.mr.gridcon.enums.PControlMode;

public class Run extends BaseState {

	private final Logger log = LoggerFactory.getLogger(Run.class);
	private boolean enableIpu1;
	private boolean enableIpu2;
	private boolean enableIpu3;
	private float offsetCurrent;
	// private ParameterSet parameterSet;

	public Run(ComponentManager manager, String gridconPcsId, String b1Id, String b2Id, String b3Id, boolean enableIpu1,
			boolean enableIpu2, boolean enableIpu3, // ParameterSet parameterSet,
			String hardRestartRelayAdress, float offsetCurrent) {
		super(manager, gridconPcsId, b1Id, b2Id, b3Id, hardRestartRelayAdress);
		this.enableIpu1 = enableIpu1;
		this.enableIpu2 = enableIpu2;
		this.enableIpu3 = enableIpu3;
		// this.parameterSet = parameterSet;
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
	public void act(GridconSettings settings) {
		this.log.info("run() -> Set all parameters to gridcon!");

		// sometimes link voltage can be too low unrecognized by gridcon, i.e. no error
		// message
		// in case of that, restart the system, but this should be detected by isError()
		// function

		this.checkBatteries();
		this.setRunParameters(settings);
		this.setStringWeighting();
		this.setOffsetCurrent();
		this.setStringControlMode();
		this.setDateAndTime();
		try {
			this.getGridconPcs().doWriteTasks();
		} catch (OpenemsNamedException e) {
			this.log.error(e.getMessage());
		}
	}

	private void checkBatteries() {
		if (getBattery1() != null) {
			if (!this.getBattery1().isStarted() && !getBattery1().hasFaults()) {
				try {
					this.getBattery1().start();
				} catch (OpenemsNamedException e) {
					System.out.println(
							"Was not able to start battery " + this.getBattery1().id() + "!\n" + e.getMessage());
				}
			}
		}
		if (getBattery2() != null) {
			if (!this.getBattery2().isStarted() && !getBattery2().hasFaults()) {
				try {
					this.getBattery2().start();
				} catch (OpenemsNamedException e) {
					System.out.println(
							"Was not able to start battery " + this.getBattery2().id() + "!\n" + e.getMessage());
				}
			}
		}
		if (getBattery3() != null) {
			if (!this.getBattery3().isStarted() && !getBattery3().hasFaults()) {
				try {
					this.getBattery3().start();
				} catch (OpenemsNamedException e) {
					System.out.println(
							"Was not able to start battery " + this.getBattery3().id() + "!\n" + e.getMessage());
				}
			}
		}
	}

	private void setOffsetCurrent() {
		if (this.hasBattery1HighestCellVoltage()) {
			this.getGridconPcs().setIRefStringA(this.offsetCurrent);
			this.getGridconPcs().setIRefStringB(0f);
			this.getGridconPcs().setIRefStringC(0f);
		}
		if (this.hasBattery2HighestCellVoltage()) {
			this.getGridconPcs().setIRefStringA(0f);
			this.getGridconPcs().setIRefStringB(this.offsetCurrent);
			this.getGridconPcs().setIRefStringC(0f);
		}
		if (this.hasBattery3HighestCellVoltage()) {
			this.getGridconPcs().setIRefStringA(0f);
			this.getGridconPcs().setIRefStringB(0f);
			this.getGridconPcs().setIRefStringC(this.offsetCurrent);
		}
	}

	private boolean hasBattery1HighestCellVoltage() {
		if (getBattery1() == null) { // battery is not existing --> cannot have highest cell voltage
			return false;
		}

		if (getBattery2() == null && getBattery3() == null) {
			// only this battery exists --> must have highest cell voltage
			return true;
		}

		if (getBattery2() == null) { // only battery one and three are existing
			return this.getMaxCellVoltage(this.getBattery1()) >= this.getMaxCellVoltage(this.getBattery3());
		}

		if (getBattery3() == null) { // only battery one and two are existing
			return this.getMaxCellVoltage(this.getBattery1()) >= this.getMaxCellVoltage(this.getBattery2());
		}

		return this.getMaxCellVoltage(getBattery1()) >= Math.max(this.getMaxCellVoltage(this.getBattery2()),
				this.getMaxCellVoltage(getBattery3()));
	}

	private boolean hasBattery2HighestCellVoltage() {
		if (this.getBattery2() == null) { // battery is not existing --> cannot have highest cell voltage
			return false;
		}

		if (this.getBattery1() == null && this.getBattery3() == null) {
			// only this battery exists --> must have highest cell voltage
			return true;
		}

		if (this.getBattery1() == null) { // only battery two and three are existing
			return this.getMaxCellVoltage(this.getBattery2()) >= this.getMaxCellVoltage(this.getBattery3());
		}

		if (this.getBattery3() == null) { // only battery one and two are existing
			return this.getMaxCellVoltage(this.getBattery2()) >= this.getMaxCellVoltage(this.getBattery1());
		}

		return this.getMaxCellVoltage(this.getBattery2()) >= Math.max(this.getMaxCellVoltage(this.getBattery1()),
				this.getMaxCellVoltage(this.getBattery3()));
	}

	private boolean hasBattery3HighestCellVoltage() {
		if (this.getBattery3() == null) { // battery is not existing --> cannot have highest cell voltage
			return false;
		}

		if (this.getBattery1() == null && this.getBattery2() == null) {
			// only this battery exists --> must have highest cell voltage
			return true;
		}

		if (this.getBattery2() == null) { // only battery one and three are existing
			return this.getMaxCellVoltage(this.getBattery3()) >= this.getMaxCellVoltage(this.getBattery1());
		}

		if (this.getBattery1() == null) { // only battery two and three are existing
			return this.getMaxCellVoltage(this.getBattery3()) >= this.getMaxCellVoltage(this.getBattery2());
		}

		return this.getMaxCellVoltage(this.getBattery3()) >= Math.max(this.getMaxCellVoltage(this.getBattery1()),
				this.getMaxCellVoltage(this.getBattery2()));
	}

	private int getMaxCellVoltage(Battery battery) {
		return battery.getMaxCellVoltage().orElse(Integer.MIN_VALUE);
	}

	private void setRunParameters(GridconSettings settings) {
		getGridconPcs().setEnableIpu1(this.enableIpu1);
		getGridconPcs().setEnableIpu2(this.enableIpu2);
		getGridconPcs().setEnableIpu3(this.enableIpu3);

		// Enable DC DC
		getGridconPcs().enableDcDc();
		getGridconPcs().setDcLinkVoltage(GridconPcs.DC_LINK_VOLTAGE_SETPOINT);

		getGridconPcs().setMode(settings.getMode());
		getGridconPcs().setU0(settings.getU0());
		getGridconPcs().setF0(settings.getF0());
		// getGridconPcs().setParameterSet(parameterSet);
		getGridconPcs().setPControlMode(PControlMode.ACTIVE_POWER_CONTROL);
		getGridconPcs().setQLimit(GridconPcs.Q_LIMIT);

		float maxPower = GridconPcsImpl.MAX_POWER_PER_INVERTER;
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
}
