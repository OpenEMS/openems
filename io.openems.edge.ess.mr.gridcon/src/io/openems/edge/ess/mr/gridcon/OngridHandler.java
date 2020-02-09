package io.openems.edge.ess.mr.gridcon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OptionsEnum;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.ess.mr.gridcon.enums.Mode;

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
		log.info("state machine run(): " + state);
		
		// Verify that we are still On-Grid -> otherwise switch to "Going Off-Grid"
		GridMode gridMode = this.parent.essGridconOngrid.getGridMode().getNextValue().asEnum();
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
			if (parent.gridconPCS.isRunning())
			return State.RUN;

			if (parent.gridconPCS.isIdle())
			return State.IDLE;

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
		if (!parent.gridconPCS.isIdle()) {
			return State.UNDEFINED;
		}

		// If no battery is ready inverter cannot start
		if (!this.parent.essGridconOngrid.isAtLeastOneBatteryReady()) {
			return State.IDLE;
		}
		
		parent.essGridconOngrid.startSystem();
		return State.IDLE;
	}

	private State doRun() throws IllegalArgumentException, OpenemsNamedException {
		// Verify State
		if (!parent.gridconPCS.isRunning()) {
			return State.UNDEFINED;
		}
		
		parent.essGridconOngrid.runSystem();

		parent.gridconPCS.setEnableIPU1(parent.essGridconOngrid.config.enableIPU1());
		parent.gridconPCS.setEnableIPU2(parent.essGridconOngrid.config.enableIPU2());
		parent.gridconPCS.setEnableIPU3(parent.essGridconOngrid.config.enableIPU3());
		parent.gridconPCS.setParameterSet(parent.essGridconOngrid.config.parameterSet());
		parent.gridconPCS.setSyncApproval(true);
		parent.gridconPCS.setBlackStartApproval(false);
		parent.gridconPCS.setModeSelection(Mode.CURRENT_CONTROL);
		parent.gridconPCS.setU0(GridconPCSImpl.ON_GRID_VOLTAGE_FACTOR);
		parent.gridconPCS.setU0(GridconPCSImpl.ON_GRID_FREQUENCY_FACTOR);

		return State.RUN;
	}

	public enum State implements OptionsEnum {
		UNDEFINED(-1, "Undefined"), //
		IDLE(1, "Idle"), //
		RUN(2, "Run"), 
//		STARTING(3, "Starting")
		;

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
