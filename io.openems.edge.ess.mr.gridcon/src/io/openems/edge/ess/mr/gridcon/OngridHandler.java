package io.openems.edge.ess.mr.gridcon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OptionsEnum;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.ess.mr.gridcon.enums.CCUState;
import io.openems.edge.ess.mr.gridcon.enums.InverterCount;
import io.openems.edge.ess.mr.gridcon.enums.PControlMode;
import io.openems.edge.ess.mr.gridcon.writeutils.CcuControlParameters;
import io.openems.edge.ess.mr.gridcon.writeutils.CommandControlRegisters;

public class OngridHandler {

	private final Logger log = LoggerFactory.getLogger(OngridHandler.class);
	private final StateMachine parent;

	private State state = State.UNDEFINED;

	public OngridHandler(StateMachine parent) {
		this.parent = parent;
	}

	public void initialize() {
		this.state = State.UNDEFINED;
	}

	protected StateMachine.State run() throws IllegalArgumentException, OpenemsNamedException {
		// Verify that we are still On-Grid -> otherwise switch to "Going Off-Grid"
		GridMode gridMode = this.parent.gridconPCS.getGridMode().getNextValue().asEnum();
		switch (gridMode) {
			case ON_GRID:
			case UNDEFINED:
				break;
			case OFF_GRID:
//			return StateMachine.State.GOING_OFFGRID;
		}

		// Always set OutputSyncDeviceBridge OFF in On-Grid state
//		this.parent.parent.setOutputSyncDeviceBridge(false);

		switch (this.state) {
		case UNDEFINED:
			this.state = this.doUndefined();
			break;

		case IDLE:
			this.state = this.doIdle();
			break;

		case RUN:
			this.state = this.doRun();
			break;
		}

		return StateMachine.State.ONGRID;
	}

	/**
	 * @return the next state
	 */
	private State doUndefined() {
		CCUState ccuState = this.parent.gridconPCS.getCcuState();
		switch (ccuState) {
		case RUN:
			return State.RUN;

		case IDLE:
			return State.IDLE;

		case ERROR:
		case DERATING_HARMONICS:
		case DERATING_POWER:
		case OVERLOAD:
		case PAUSE:
		case PRECHARGE:
		case READY:
		case SHORT_CIRCUIT_DETECTED:
		case SIA_ACTIVE:
		case STOP_PRECHARGE:
		case UNDEFINED:
		case VOLTAGE_RAMPING_UP:
			this.log.info("Unhandled On-Grid state [" + ccuState.toString() + "]");
			break;
		}
		return State.UNDEFINED;
	}

	/**
	 * Handles idle operation in On-Grid -> tries to start the inverter.
	 * 
	 * @return the next state
	 * @throws OpenemsNamedException
	 * @throws IllegalArgumentException
	 */
	private State doIdle() throws IllegalArgumentException, OpenemsNamedException {
		// Verify State
		CCUState ccuState = this.parent.gridconPCS.getCcuState();
		if (ccuState != CCUState.IDLE) {
			return State.UNDEFINED;
		}

		// If no battery is ready inverter cannot start
		if (!this.parent.onGridController.isAtLeastOneBatteryReady()) {
			return State.IDLE;
		}

		InverterCount inverterCount = this.parent.gridconPCS.config.inverterCount();
		boolean enableIPU1 = this.parent.gridconPCS.config.enableIPU1();
		boolean enableIPU2 = this.parent.gridconPCS.config.enableIPU2();
		boolean enableIPU3 = this.parent.gridconPCS.config.enableIPU3();
		new CommandControlRegisters() //
				.play(true) // Start system
				.syncApproval(true) //
				.blackstartApproval(false) //
				.shortCircuitHandling(true) //
				.modeSelection(CommandControlRegisters.Mode.CURRENT_CONTROL) //
				.parameterSet1(true) //
				.parameterU0(GridconPCS.ON_GRID_VOLTAGE_FACTOR) //
				.parameterF0(GridconPCS.ON_GRID_FREQUENCY_FACTOR) //
				.enableIpus(inverterCount, enableIPU1, enableIPU2, enableIPU3) //
				.writeToChannels(this.parent.gridconPCS);
		new CcuControlParameters() //
				.pControlMode(PControlMode.ACTIVE_POWER_CONTROL) //
				.qLimit(1f) //
				.writeToChannels(this.parent.gridconPCS);
		this.parent.gridconPCS.setIpuControlSettings();

		return State.IDLE;
	}

	private State doRun() throws IllegalArgumentException, OpenemsNamedException {
		// Verify State
		CCUState ccuState = this.parent.gridconPCS.getCcuState();
		if (ccuState != CCUState.RUN) {
			return State.UNDEFINED;
		}

		InverterCount inverterCount = this.parent.gridconPCS.config.inverterCount();
		boolean enableIPU1 = this.parent.gridconPCS.config.enableIPU1();
		boolean enableIPU2 = this.parent.gridconPCS.config.enableIPU2();
		boolean enableIPU3 = this.parent.gridconPCS.config.enableIPU3();
		new CommandControlRegisters() //
				.syncApproval(true) //
				.blackstartApproval(false) //
				.shortCircuitHandling(true) //
				.modeSelection(CommandControlRegisters.Mode.CURRENT_CONTROL) //
				.parameterSet1(true) //
				.parameterU0(GridconPCS.ON_GRID_VOLTAGE_FACTOR) //
				.parameterF0(GridconPCS.ON_GRID_FREQUENCY_FACTOR) //
				.enableIpus(inverterCount, enableIPU1, enableIPU2, enableIPU3) //
				.writeToChannels(this.parent.gridconPCS);
		new CcuControlParameters() //
				.pControlMode(PControlMode.ACTIVE_POWER_CONTROL) //
				.qLimit(1f) //
				.writeToChannels(this.parent.gridconPCS);
		this.parent.gridconPCS.setIpuControlSettings();

		return State.RUN;
	}

	public State getState() {
		return state;
	}

	public enum State implements OptionsEnum {
		UNDEFINED(-1, "Undefined"), //
		IDLE(1, "Idle"), //
		RUN(2, "Run");

		private final int value;
		private final String name;

		private State(int value, String name) {
			this.value = value;
			this.name = name;
		}

		@Override
		public int getValue() {
			return value;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public OptionsEnum getUndefined() {
			return UNDEFINED;
		}
	}
}
